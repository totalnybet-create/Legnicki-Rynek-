import { createHash, randomBytes, randomUUID, scrypt, timingSafeEqual } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { stat, readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { dirname, extname, join, resolve } from 'node:path';
import { promisify } from 'node:util';
import { DatabaseSync } from 'node:sqlite';

const scryptAsync = promisify(scrypt);
const JSON_LIMIT_BYTES = 1_000_000;
const DEFAULT_UPLOAD_LIMIT_BYTES = 10 * 1024 * 1024;
const ACCESS_TOKEN_LIFETIME_MS = 30 * 24 * 60 * 60 * 1000;
const REFRESH_TOKEN_LIFETIME_MS = 90 * 24 * 60 * 60 * 1000;
const MAX_LISTING_IMAGES = 12;

class HttpError extends Error {
  constructor(statusCode, message, code = 'request_error', details = undefined) {
    super(message);
    this.name = 'HttpError';
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
  }
}

class SlidingWindowRateLimiter {
  constructor({ limit, windowMs }) {
    this.limit = limit;
    this.windowMs = windowMs;
    this.entries = new Map();
  }

  check(key) {
    const now = Date.now();
    const current = this.entries.get(key);
    if (!current || current.resetAt <= now) {
      this.entries.set(key, { count: 1, resetAt: now + this.windowMs });
      return;
    }
    if (current.count >= this.limit) {
      const retryAfterSeconds = Math.max(1, Math.ceil((current.resetAt - now) / 1000));
      const error = new HttpError(
        429,
        'Zbyt wiele prób. Spróbuj ponownie później.',
        'rate_limit_exceeded'
      );
      error.retryAfterSeconds = retryAfterSeconds;
      throw error;
    }
    current.count += 1;
  }
}

export function createLegnickiRynekApi(options = {}) {
  const dbPath = resolve(options.dbPath ?? './data/legnicki-rynek.sqlite');
  const uploadDir = resolve(options.uploadDir ?? './data/uploads');
  const configuredPublicBaseUrl = String(options.publicBaseUrl ?? '').replace(/\/$/, '');
  const maxUploadBytes = clampInteger(
    options.maxUploadBytes ?? DEFAULT_UPLOAD_LIMIT_BYTES,
    1_000_000,
    25 * 1024 * 1024
  );
  const allowedOrigin = String(options.allowedOrigin ?? '').trim();

  mkdirSync(dirname(dbPath), { recursive: true });
  mkdirSync(uploadDir, { recursive: true });

  const database = new DatabaseSync(dbPath);
  configureDatabase(database);
  migrateDatabase(database);

  const authRateLimiter = new SlidingWindowRateLimiter({
    limit: 12,
    windowMs: 15 * 60 * 1000
  });
  const uploadRateLimiter = new SlidingWindowRateLimiter({
    limit: 40,
    windowMs: 60 * 60 * 1000
  });

  const server = createServer(async (request, response) => {
    const startedAt = Date.now();
    const requestId = randomUUID();
    response.setHeader('X-Request-Id', requestId);
    response.setHeader('X-Content-Type-Options', 'nosniff');
    response.setHeader('X-Frame-Options', 'DENY');
    response.setHeader('Referrer-Policy', 'no-referrer');
    response.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
    if (allowedOrigin) {
      response.setHeader('Access-Control-Allow-Origin', allowedOrigin);
      response.setHeader('Vary', 'Origin');
      response.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type');
      response.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    }

    let statusCode = 500;
    try {
      if (request.method === 'OPTIONS' && allowedOrigin) {
        response.writeHead(204);
        response.end();
        statusCode = 204;
        return;
      }

      const requestUrl = new URL(request.url ?? '/', 'http://localhost');
      const path = requestUrl.pathname;
      const method = request.method ?? 'GET';
      const clientIp = clientAddress(request);

      if (method === 'GET' && path === '/health') {
        statusCode = 200;
        return sendJson(response, 200, {
          status: 'ok',
          service: 'legnicki-rynek-api',
          time: new Date().toISOString()
        });
      }

      if (method === 'POST' && path === '/auth/register') {
        authRateLimiter.check(`register:${clientIp}`);
        const body = await readJson(request);
        const result = await registerUser(database, body);
        statusCode = 201;
        return sendJson(response, 201, result);
      }

      if (method === 'POST' && path === '/auth/login') {
        authRateLimiter.check(`login:${clientIp}`);
        const body = await readJson(request);
        const result = await loginUser(database, body);
        statusCode = 200;
        return sendJson(response, 200, result);
      }

      if (method === 'POST' && path === '/auth/refresh') {
        authRateLimiter.check(`refresh:${clientIp}`);
        const body = await readJson(request);
        const result = refreshSession(database, body);
        statusCode = 200;
        return sendJson(response, 200, result);
      }

      if (method === 'POST' && path === '/auth/logout') {
        const session = authenticateRequest(database, request);
        database.prepare('DELETE FROM sessions WHERE id = ?').run(session.sessionId);
        statusCode = 204;
        response.writeHead(204);
        response.end();
        return;
      }

      if (method === 'GET' && path === '/listings') {
        const listings = database.prepare(`
          SELECT id, title, price, location, category_id, description, image_urls,
                 owner_id, seller_name, created_at, updated_at, status, latitude, longitude
          FROM listings
          ORDER BY created_at DESC, updated_at DESC
          LIMIT 5000
        `).all().map(databaseListingToApi);
        statusCode = 200;
        return sendJson(response, 200, { listings });
      }

      const listingMatch = path.match(/^\/listings\/([^/]+)$/);
      if (listingMatch && method === 'PUT') {
        const session = authenticateRequest(database, request);
        const listingId = decodePathSegment(listingMatch[1], 160);
        const body = await readJson(request);
        const listing = upsertListing(database, listingId, body, session.user);
        statusCode = 200;
        return sendJson(response, 200, listing);
      }

      if (listingMatch && method === 'DELETE') {
        const session = authenticateRequest(database, request);
        const listingId = decodePathSegment(listingMatch[1], 160);
        deleteListing(database, listingId, session.user.id);
        statusCode = 204;
        response.writeHead(204);
        response.end();
        return;
      }

      if (method === 'POST' && path === '/uploads') {
        uploadRateLimiter.check(`upload:${clientIp}`);
        const session = authenticateRequest(database, request);
        const uploaded = await receiveImageUpload({
          request,
          database,
          userId: session.user.id,
          uploadDir,
          maxUploadBytes,
          publicBaseUrl: configuredPublicBaseUrl || derivePublicBaseUrl(request)
        });
        statusCode = 201;
        return sendJson(response, 201, uploaded);
      }

      const uploadMatch = path.match(/^\/uploads\/([A-Za-z0-9._-]+)$/);
      if (uploadMatch && method === 'GET') {
        const filename = uploadMatch[1];
        const record = database.prepare(
          'SELECT mime_type FROM uploads WHERE filename = ? LIMIT 1'
        ).get(filename);
        if (!record) throw new HttpError(404, 'Nie znaleziono zdjęcia.', 'not_found');
        const filePath = safeUploadPath(uploadDir, filename);
        const fileStat = await stat(filePath).catch(() => null);
        if (!fileStat?.isFile()) {
          throw new HttpError(404, 'Nie znaleziono zdjęcia.', 'not_found');
        }
        const bytes = await readFile(filePath);
        response.setHeader('Content-Type', record.mime_type);
        response.setHeader('Content-Length', String(bytes.length));
        response.setHeader('Cache-Control', 'public, max-age=31536000, immutable');
        statusCode = 200;
        response.writeHead(200);
        response.end(bytes);
        return;
      }

      throw new HttpError(404, 'Nie znaleziono endpointu.', 'not_found');
    } catch (error) {
      const normalized = normalizeError(error);
      statusCode = normalized.statusCode;
      if (normalized.retryAfterSeconds) {
        response.setHeader('Retry-After', String(normalized.retryAfterSeconds));
      }
      if (!response.headersSent) {
        sendJson(response, normalized.statusCode, {
          error: normalized.code,
          message: normalized.message,
          requestId,
          ...(normalized.details === undefined ? {} : { details: normalized.details })
        });
      } else {
        response.destroy();
      }
    } finally {
      const method = request.method ?? 'GET';
      const path = new URL(request.url ?? '/', 'http://localhost').pathname;
      console.info(JSON.stringify({
        requestId,
        method,
        path,
        statusCode,
        durationMs: Date.now() - startedAt
      }));
    }
  });

  return {
    server,
    database,
    close: async () => {
      if (server.listening) {
        await new Promise((resolveClose, rejectClose) => {
          server.close((error) => error ? rejectClose(error) : resolveClose());
        });
      }
      database.close();
    }
  };
}

function configureDatabase(database) {
  database.exec('PRAGMA journal_mode = WAL');
  database.exec('PRAGMA foreign_keys = ON');
  database.exec('PRAGMA busy_timeout = 5000');
}

function migrateDatabase(database) {
  database.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE COLLATE NOCASE,
      password_salt TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS sessions (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      access_token_hash TEXT NOT NULL UNIQUE,
      refresh_token_hash TEXT NOT NULL UNIQUE,
      access_expires_at INTEGER NOT NULL,
      refresh_expires_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    );
    CREATE INDEX IF NOT EXISTS index_sessions_access_token_hash
      ON sessions(access_token_hash);
    CREATE INDEX IF NOT EXISTS index_sessions_refresh_token_hash
      ON sessions(refresh_token_hash);
    CREATE INDEX IF NOT EXISTS index_sessions_user_id
      ON sessions(user_id);

    CREATE TABLE IF NOT EXISTS listings (
      id TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      price INTEGER NOT NULL,
      location TEXT NOT NULL,
      category_id TEXT NOT NULL,
      description TEXT NOT NULL,
      image_urls TEXT NOT NULL,
      owner_id TEXT NOT NULL,
      seller_name TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL,
      status TEXT NOT NULL,
      latitude REAL,
      longitude REAL,
      FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE CASCADE
    );
    CREATE INDEX IF NOT EXISTS index_listings_owner_id ON listings(owner_id);
    CREATE INDEX IF NOT EXISTS index_listings_created_at ON listings(created_at);
    CREATE INDEX IF NOT EXISTS index_listings_category_id ON listings(category_id);
    CREATE INDEX IF NOT EXISTS index_listings_status ON listings(status);

    CREATE TABLE IF NOT EXISTS uploads (
      filename TEXT PRIMARY KEY,
      owner_id TEXT NOT NULL,
      mime_type TEXT NOT NULL,
      size_bytes INTEGER NOT NULL,
      created_at INTEGER NOT NULL,
      FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);
}

async function registerUser(database, input) {
  const name = requiredText(input.name, 'name', 2, 80);
  const email = normalizeEmail(input.email);
  const password = validatePassword(input.password);
  const existing = database.prepare('SELECT id FROM users WHERE email = ? LIMIT 1').get(email);
  if (existing) {
    throw new HttpError(409, 'Nie można utworzyć konta dla tego adresu.', 'account_unavailable');
  }

  const salt = randomBytes(16);
  const passwordHash = await derivePasswordHash(password, salt);
  const user = {
    id: randomUUID(),
    name,
    email,
    createdAt: Date.now()
  };
  database.prepare(`
    INSERT INTO users(id, name, email, password_salt, password_hash, created_at)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(
    user.id,
    user.name,
    user.email,
    salt.toString('base64'),
    passwordHash.toString('base64'),
    user.createdAt
  );

  return sessionResponse(user, createSession(database, user.id));
}

async function loginUser(database, input) {
  const email = normalizeEmail(input.email);
  const password = validatePassword(input.password);
  const row = database.prepare(`
    SELECT id, name, email, password_salt, password_hash, created_at
    FROM users WHERE email = ? LIMIT 1
  `).get(email);
  if (!row) {
    await fakePasswordWork(password);
    throw new HttpError(401, 'Nieprawidłowy e-mail lub hasło.', 'invalid_credentials');
  }

  const salt = Buffer.from(row.password_salt, 'base64');
  const expected = Buffer.from(row.password_hash, 'base64');
  const actual = await derivePasswordHash(password, salt);
  if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) {
    throw new HttpError(401, 'Nieprawidłowy e-mail lub hasło.', 'invalid_credentials');
  }

  cleanupExpiredSessions(database);
  const user = {
    id: row.id,
    name: row.name,
    email: row.email,
    createdAt: row.created_at
  };
  return sessionResponse(user, createSession(database, user.id));
}

function refreshSession(database, input) {
  const refreshToken = requiredText(input.refreshToken, 'refreshToken', 16, 8192);
  const tokenHash = hashToken(refreshToken);
  const row = database.prepare(`
    SELECT s.id AS session_id, s.user_id, s.refresh_expires_at,
           u.id, u.name, u.email, u.created_at
    FROM sessions s
    JOIN users u ON u.id = s.user_id
    WHERE s.refresh_token_hash = ?
    LIMIT 1
  `).get(tokenHash);
  if (!row || row.refresh_expires_at <= Date.now()) {
    if (row) database.prepare('DELETE FROM sessions WHERE id = ?').run(row.session_id);
    throw new HttpError(401, 'Sesja wygasła. Zaloguj się ponownie.', 'session_expired');
  }

  database.prepare('DELETE FROM sessions WHERE id = ?').run(row.session_id);
  const user = {
    id: row.id,
    name: row.name,
    email: row.email,
    createdAt: row.created_at
  };
  return sessionResponse(user, createSession(database, row.user_id));
}

function createSession(database, userId) {
  const now = Date.now();
  const accessToken = randomBytes(32).toString('base64url');
  const refreshToken = randomBytes(48).toString('base64url');
  const session = {
    id: randomUUID(),
    accessToken,
    refreshToken,
    accessExpiresAt: now + ACCESS_TOKEN_LIFETIME_MS,
    refreshExpiresAt: now + REFRESH_TOKEN_LIFETIME_MS
  };
  database.prepare(`
    INSERT INTO sessions(
      id, user_id, access_token_hash, refresh_token_hash,
      access_expires_at, refresh_expires_at, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(
    session.id,
    userId,
    hashToken(accessToken),
    hashToken(refreshToken),
    session.accessExpiresAt,
    session.refreshExpiresAt,
    now
  );
  return session;
}

function sessionResponse(user, session) {
  return {
    user: {
      id: user.id,
      name: user.name,
      email: user.email
    },
    session: {
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
      expiresAt: session.accessExpiresAt
    }
  };
}

function authenticateRequest(database, request) {
  const header = String(request.headers.authorization ?? '');
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) throw new HttpError(401, 'Wymagane jest zalogowanie.', 'authentication_required');
  const token = match[1].trim();
  if (token.length < 16 || token.length > 8192) {
    throw new HttpError(401, 'Nieprawidłowa sesja.', 'invalid_session');
  }
  const row = database.prepare(`
    SELECT s.id AS session_id, s.access_expires_at,
           u.id, u.name, u.email, u.created_at
    FROM sessions s
    JOIN users u ON u.id = s.user_id
    WHERE s.access_token_hash = ?
    LIMIT 1
  `).get(hashToken(token));
  if (!row || row.access_expires_at <= Date.now()) {
    if (row) database.prepare('DELETE FROM sessions WHERE id = ?').run(row.session_id);
    throw new HttpError(401, 'Sesja wygasła. Zaloguj się ponownie.', 'session_expired');
  }
  return {
    sessionId: row.session_id,
    user: {
      id: row.id,
      name: row.name,
      email: row.email,
      createdAt: row.created_at
    }
  };
}

function upsertListing(database, listingId, input, user) {
  const sanitized = sanitizeListingInput(input);
  const existing = database.prepare(`
    SELECT id, owner_id, created_at, updated_at
    FROM listings WHERE id = ? LIMIT 1
  `).get(listingId);
  if (existing && existing.owner_id !== user.id) {
    throw new HttpError(403, 'Nie możesz zmienić cudzego ogłoszenia.', 'forbidden');
  }
  if (existing && sanitized.updatedAt < existing.updated_at) {
    const current = database.prepare(`
      SELECT id, title, price, location, category_id, description, image_urls,
             owner_id, seller_name, created_at, updated_at, status, latitude, longitude
      FROM listings WHERE id = ? LIMIT 1
    `).get(listingId);
    throw new HttpError(
      409,
      'Na serwerze istnieje nowsza wersja ogłoszenia.',
      'listing_conflict',
      { listing: databaseListingToApi(current) }
    );
  }

  const now = Date.now();
  const createdAt = existing?.created_at ?? sanitized.createdAt ?? now;
  const updatedAt = Math.max(now, sanitized.updatedAt);
  database.prepare(`
    INSERT INTO listings(
      id, title, price, location, category_id, description, image_urls,
      owner_id, seller_name, created_at, updated_at, status, latitude, longitude
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      title = excluded.title,
      price = excluded.price,
      location = excluded.location,
      category_id = excluded.category_id,
      description = excluded.description,
      image_urls = excluded.image_urls,
      seller_name = excluded.seller_name,
      updated_at = excluded.updated_at,
      status = excluded.status,
      latitude = excluded.latitude,
      longitude = excluded.longitude
  `).run(
    listingId,
    sanitized.title,
    sanitized.price,
    sanitized.location,
    sanitized.categoryId,
    sanitized.description,
    JSON.stringify(sanitized.imageUrls),
    user.id,
    user.name,
    createdAt,
    updatedAt,
    sanitized.status,
    sanitized.latitude,
    sanitized.longitude
  );

  const saved = database.prepare(`
    SELECT id, title, price, location, category_id, description, image_urls,
           owner_id, seller_name, created_at, updated_at, status, latitude, longitude
    FROM listings WHERE id = ? LIMIT 1
  `).get(listingId);
  return databaseListingToApi(saved);
}

function deleteListing(database, listingId, userId) {
  const existing = database.prepare(
    'SELECT owner_id FROM listings WHERE id = ? LIMIT 1'
  ).get(listingId);
  if (!existing) return;
  if (existing.owner_id !== userId) {
    throw new HttpError(403, 'Nie możesz usunąć cudzego ogłoszenia.', 'forbidden');
  }
  database.prepare('DELETE FROM listings WHERE id = ?').run(listingId);
}

function sanitizeListingInput(input) {
  const status = String(input.status ?? 'ACTIVE').toUpperCase();
  if (!['ACTIVE', 'RESERVED', 'SOLD', 'EXPIRED'].includes(status)) {
    throw new HttpError(400, 'Nieprawidłowy status ogłoszenia.', 'validation_error');
  }
  const price = Number(input.price);
  if (!Number.isSafeInteger(price) || price < 0 || price > 100_000_000) {
    throw new HttpError(400, 'Nieprawidłowa cena.', 'validation_error');
  }
  const imageUrls = Array.isArray(input.imageUrls)
    ? input.imageUrls
        .map((value) => String(value).trim())
        .filter(isPublicHttpUrl)
        .filter((value, index, array) => array.indexOf(value) === index)
        .slice(0, MAX_LISTING_IMAGES)
    : [];
  const createdAt = optionalTimestamp(input.createdAt);
  const updatedAt = optionalTimestamp(input.updatedAt) ?? Date.now();
  return {
    title: requiredText(input.title, 'title', 3, 140),
    price,
    location: requiredText(input.location, 'location', 2, 180),
    categoryId: requiredText(input.categoryId, 'categoryId', 1, 80),
    description: requiredText(input.description, 'description', 10, 5000),
    imageUrls,
    createdAt,
    updatedAt,
    status,
    latitude: optionalCoordinate(input.latitude, -90, 90),
    longitude: optionalCoordinate(input.longitude, -180, 180)
  };
}

function databaseListingToApi(row) {
  return {
    apiVersion: 1,
    id: row.id,
    title: row.title,
    price: row.price,
    location: row.location,
    categoryId: row.category_id,
    description: row.description,
    imageUrls: safeJsonArray(row.image_urls),
    ownerId: row.owner_id,
    sellerName: row.seller_name,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    status: row.status,
    ...(row.latitude == null ? {} : { latitude: row.latitude }),
    ...(row.longitude == null ? {} : { longitude: row.longitude })
  };
}

async function receiveImageUpload({
  request,
  database,
  userId,
  uploadDir,
  maxUploadBytes,
  publicBaseUrl
}) {
  const contentType = String(request.headers['content-type'] ?? '');
  const boundaryMatch = contentType.match(/boundary=(?:"([^"]+)"|([^;]+))/i);
  const boundary = (boundaryMatch?.[1] ?? boundaryMatch?.[2] ?? '').trim();
  if (!boundary || boundary.length > 200) {
    throw new HttpError(400, 'Nieprawidłowe żądanie multipart.', 'invalid_multipart');
  }
  const body = await readBody(request, maxUploadBytes + 64 * 1024);
  const file = parseSingleMultipartFile(body, boundary, maxUploadBytes);
  const detected = detectImageType(file.bytes, file.mimeType);
  const filename = `${randomUUID()}${detected.extension}`;
  const filePath = safeUploadPath(uploadDir, filename);
  writeFileSync(filePath, file.bytes, { flag: 'wx', mode: 0o600 });
  database.prepare(`
    INSERT INTO uploads(filename, owner_id, mime_type, size_bytes, created_at)
    VALUES (?, ?, ?, ?, ?)
  `).run(filename, userId, detected.mimeType, file.bytes.length, Date.now());

  return { url: `${publicBaseUrl}/uploads/${filename}` };
}

function parseSingleMultipartFile(body, boundary, maxUploadBytes) {
  const boundaryBuffer = Buffer.from(`--${boundary}`);
  const firstBoundary = body.indexOf(boundaryBuffer);
  if (firstBoundary < 0) {
    throw new HttpError(400, 'Nie znaleziono granicy multipart.', 'invalid_multipart');
  }
  const headerStart = firstBoundary + boundaryBuffer.length + 2;
  const headerEnd = body.indexOf(Buffer.from('\r\n\r\n'), headerStart);
  if (headerEnd < 0 || headerEnd - headerStart > 16_384) {
    throw new HttpError(400, 'Nieprawidłowe nagłówki pliku.', 'invalid_multipart');
  }
  const headers = body.subarray(headerStart, headerEnd).toString('utf8');
  const disposition = headers.match(/content-disposition:\s*form-data;[^\r\n]*/i)?.[0] ?? '';
  if (!/name="file"/i.test(disposition)) {
    throw new HttpError(400, 'Brak pola pliku o nazwie file.', 'invalid_multipart');
  }
  const mimeType = headers.match(/content-type:\s*([^\r\n]+)/i)?.[1]?.trim().toLowerCase() ?? '';
  if (!mimeType.startsWith('image/')) {
    throw new HttpError(415, 'Dozwolone są wyłącznie obrazy.', 'unsupported_media_type');
  }
  const dataStart = headerEnd + 4;
  const closingMarker = Buffer.from(`\r\n--${boundary}`);
  const dataEnd = body.indexOf(closingMarker, dataStart);
  if (dataEnd < 0) {
    throw new HttpError(400, 'Nieprawidłowe zakończenie pliku.', 'invalid_multipart');
  }
  const bytes = body.subarray(dataStart, dataEnd);
  if (bytes.length === 0 || bytes.length > maxUploadBytes) {
    throw new HttpError(413, 'Zdjęcie jest puste lub przekracza limit.', 'upload_too_large');
  }
  return { mimeType, bytes };
}

function detectImageType(bytes, declaredMimeType) {
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return { mimeType: 'image/jpeg', extension: '.jpg' };
  }
  if (bytes.length >= 8 && bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]))) {
    return { mimeType: 'image/png', extension: '.png' };
  }
  if (bytes.length >= 12 && bytes.subarray(0, 4).toString('ascii') === 'RIFF' && bytes.subarray(8, 12).toString('ascii') === 'WEBP') {
    return { mimeType: 'image/webp', extension: '.webp' };
  }
  if (bytes.length >= 6 && ['GIF87a', 'GIF89a'].includes(bytes.subarray(0, 6).toString('ascii'))) {
    return { mimeType: 'image/gif', extension: '.gif' };
  }
  if (bytes.length >= 12 && bytes.subarray(4, 8).toString('ascii') === 'ftyp') {
    const brand = bytes.subarray(8, 12).toString('ascii').toLowerCase();
    if (['heic', 'heix', 'hevc', 'hevx', 'mif1', 'msf1'].includes(brand)) {
      return { mimeType: 'image/heic', extension: '.heic' };
    }
    if (brand === 'avif') {
      return { mimeType: 'image/avif', extension: '.avif' };
    }
  }
  throw new HttpError(
    415,
    `Nieobsługiwany format obrazu (${declaredMimeType || 'nieznany'}).`,
    'unsupported_media_type'
  );
}

async function readJson(request) {
  const contentType = String(request.headers['content-type'] ?? '');
  if (!contentType.toLowerCase().startsWith('application/json')) {
    throw new HttpError(415, 'Wymagany jest Content-Type application/json.', 'unsupported_media_type');
  }
  const bytes = await readBody(request, JSON_LIMIT_BYTES);
  try {
    const value = JSON.parse(bytes.toString('utf8'));
    if (!value || Array.isArray(value) || typeof value !== 'object') {
      throw new Error('not_object');
    }
    return value;
  } catch {
    throw new HttpError(400, 'Nieprawidłowy JSON.', 'invalid_json');
  }
}

async function readBody(request, limitBytes) {
  const chunks = [];
  let total = 0;
  for await (const chunk of request) {
    const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
    total += buffer.length;
    if (total > limitBytes) {
      throw new HttpError(413, 'Treść żądania jest zbyt duża.', 'payload_too_large');
    }
    chunks.push(buffer);
  }
  return Buffer.concat(chunks, total);
}

function sendJson(response, statusCode, body) {
  const json = JSON.stringify(body);
  response.setHeader('Content-Type', 'application/json; charset=utf-8');
  response.setHeader('Content-Length', String(Buffer.byteLength(json)));
  response.setHeader('Cache-Control', 'no-store');
  response.writeHead(statusCode);
  response.end(json);
}

function requiredText(value, field, minimumLength, maximumLength) {
  const text = String(value ?? '').trim();
  if (text.length < minimumLength || text.length > maximumLength) {
    throw new HttpError(
      400,
      `Pole ${field} musi mieć od ${minimumLength} do ${maximumLength} znaków.`,
      'validation_error'
    );
  }
  return text;
}

function normalizeEmail(value) {
  const email = String(value ?? '').trim().toLowerCase().slice(0, 160);
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new HttpError(400, 'Podaj prawidłowy adres e-mail.', 'validation_error');
  }
  return email;
}

function validatePassword(value) {
  const password = String(value ?? '');
  if (password.length < 8 || password.length > 128) {
    throw new HttpError(400, 'Hasło musi mieć od 8 do 128 znaków.', 'validation_error');
  }
  return password;
}

async function derivePasswordHash(password, salt) {
  return Buffer.from(await scryptAsync(password, salt, 64, {
    N: 16_384,
    r: 8,
    p: 1,
    maxmem: 64 * 1024 * 1024
  }));
}

async function fakePasswordWork(password) {
  await derivePasswordHash(password, Buffer.alloc(16, 7));
}

function hashToken(token) {
  return createHash('sha256').update(token).digest('hex');
}

function optionalTimestamp(value) {
  if (value == null || value === '') return null;
  const number = Number(value);
  if (!Number.isSafeInteger(number) || number <= 0 || number > Date.now() + 24 * 60 * 60 * 1000) {
    throw new HttpError(400, 'Nieprawidłowy znacznik czasu.', 'validation_error');
  }
  return number;
}

function optionalCoordinate(value, minimum, maximum) {
  if (value == null || value === '') return null;
  const number = Number(value);
  if (!Number.isFinite(number) || number < minimum || number > maximum) {
    throw new HttpError(400, 'Nieprawidłowe współrzędne.', 'validation_error');
  }
  return number;
}

function isPublicHttpUrl(value) {
  try {
    const url = new URL(value);
    return ['https:', 'http:'].includes(url.protocol) && Boolean(url.hostname);
  } catch {
    return false;
  }
}

function safeJsonArray(value) {
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

function cleanupExpiredSessions(database) {
  database.prepare('DELETE FROM sessions WHERE refresh_expires_at <= ?').run(Date.now());
}

function decodePathSegment(value, maximumLength) {
  let decoded;
  try {
    decoded = decodeURIComponent(value);
  } catch {
    throw new HttpError(400, 'Nieprawidłowy identyfikator.', 'validation_error');
  }
  if (!decoded || decoded.length > maximumLength || decoded.includes('/')) {
    throw new HttpError(400, 'Nieprawidłowy identyfikator.', 'validation_error');
  }
  return decoded;
}

function safeUploadPath(uploadDir, filename) {
  if (!/^[A-Za-z0-9._-]+$/.test(filename)) {
    throw new HttpError(400, 'Nieprawidłowa nazwa pliku.', 'validation_error');
  }
  const base = resolve(uploadDir);
  const target = resolve(join(base, filename));
  if (!target.startsWith(`${base}/`) && target !== base) {
    throw new HttpError(400, 'Nieprawidłowa ścieżka pliku.', 'validation_error');
  }
  return target;
}

function derivePublicBaseUrl(request) {
  const forwardedProto = String(request.headers['x-forwarded-proto'] ?? '')
    .split(',')[0]
    .trim();
  const protocol = forwardedProto === 'https' ? 'https' : 'http';
  const host = String(request.headers.host ?? 'localhost');
  return `${protocol}://${host}`;
}

function clientAddress(request) {
  const forwarded = String(request.headers['x-forwarded-for'] ?? '')
    .split(',')[0]
    .trim();
  return forwarded || request.socket.remoteAddress || 'unknown';
}

function clampInteger(value, minimum, maximum) {
  const number = Number(value);
  if (!Number.isFinite(number)) return minimum;
  return Math.min(maximum, Math.max(minimum, Math.trunc(number)));
}

function normalizeError(error) {
  if (error instanceof HttpError) return error;
  console.error(error);
  return new HttpError(500, 'Wewnętrzny błąd serwera.', 'internal_error');
}
