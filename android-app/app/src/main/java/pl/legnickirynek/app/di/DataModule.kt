package pl.legnickirynek.app.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pl.legnickirynek.app.core.database.LegnickiRynekDatabase
import pl.legnickirynek.app.core.database.ListingDao
import pl.legnickirynek.app.data.repository.OfflineFirstMarketplaceRepository
import pl.legnickirynek.app.domain.repository.MarketplaceRepository

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LegnickiRynekDatabase = Room.databaseBuilder(
        context,
        LegnickiRynekDatabase::class.java,
        "legnicki_rynek.db"
    ).build()

    @Provides
    fun provideListingDao(database: LegnickiRynekDatabase): ListingDao =
        database.listingDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMarketplaceRepository(
        repository: OfflineFirstMarketplaceRepository
    ): MarketplaceRepository
}
