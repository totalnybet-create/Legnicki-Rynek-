package pl.legnickirynek.app.data

import pl.legnickirynek.app.model.Category
import pl.legnickirynek.app.model.Conversation
import pl.legnickirynek.app.model.Listing
import pl.legnickirynek.app.model.LocalEvent

object SampleData {
    val categories = listOf(
        Category("motoryzacja", "Motoryzacja", "🚗"),
        Category("nieruchomosci", "Nieruchomości", "🏠"),
        Category("praca", "Praca", "💼"),
        Category("uslugi", "Usługi", "🛠"),
        Category("dom-ogrod", "Dom i ogród", "🪴"),
        Category("elektronika", "Elektronika", "📱"),
        Category("moda", "Moda", "👕"),
        Category("sport", "Sport i hobby", "⚽")
    )

    val listings = listOf(
        Listing(
            id = "rower-miejski",
            title = "Rower miejski w dobrym stanie",
            price = 850,
            location = "Legnica, Centrum",
            categoryId = "sport",
            description = "Sprawny rower miejski po przeglądzie. Oświetlenie i bagażnik w komplecie."
        ),
        Listing(
            id = "mieszkanie-tarninow",
            title = "Mieszkanie 2 pokoje na Tarninowie",
            price = 2300,
            location = "Legnica, Tarninów",
            categoryId = "nieruchomosci",
            description = "Umeblowane mieszkanie blisko centrum. Cena miesięczna plus media."
        ),
        Listing(
            id = "thinkpad",
            title = "Laptop Lenovo ThinkPad",
            price = 1450,
            location = "Legnica, Piekary",
            categoryId = "elektronika",
            description = "Laptop biznesowy z dyskiem SSD, 16 GB RAM i oryginalnym zasilaczem."
        ),
        Listing(
            id = "opony-zimowe",
            title = "Komplet opon zimowych 205/55 R16",
            price = 700,
            location = "Legnica",
            categoryId = "motoryzacja",
            description = "Cztery opony w dobrym stanie. Możliwy odbiór jeszcze dziś."
        ),
        Listing(
            id = "malowanie",
            title = "Malowanie mieszkań i drobne remonty",
            price = 45,
            location = "Legnica i okolice",
            categoryId = "uslugi",
            description = "Wycena bezpłatna. Terminowe wykonanie i porządek po zakończeniu prac."
        )
    )

    val events = listOf(
        LocalEvent(
            id = "jarmark",
            title = "Lokalny jarmark na rynku",
            date = "Sobota, 10:00",
            location = "Rynek w Legnicy",
            description = "Stoiska lokalnych wystawców, rękodzieło i atrakcje rodzinne."
        ),
        LocalEvent(
            id = "koncert",
            title = "Koncert plenerowy",
            date = "Niedziela, 18:00",
            location = "Park Miejski",
            description = "Bezpłatny koncert lokalnych zespołów."
        )
    )

    val conversations = listOf(
        Conversation(
            id = "c1",
            person = "Marek",
            listingTitle = "Rower miejski w dobrym stanie",
            lastMessage = "Czy ogłoszenie jest jeszcze aktualne?",
            time = "12:42",
            unread = true
        ),
        Conversation(
            id = "c2",
            person = "Anna",
            listingTitle = "Laptop Lenovo ThinkPad",
            lastMessage = "Mogę odebrać jutro po 17:00.",
            time = "Wczoraj",
            unread = false
        )
    )
}
