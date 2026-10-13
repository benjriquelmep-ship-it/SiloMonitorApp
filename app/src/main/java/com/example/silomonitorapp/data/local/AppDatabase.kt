package com.example.silomonitorapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Base de datos local Room (SQLite). Registra las 3 tablas de la app.
// Si cambia una entidad hay que subir 'version'.
@Database(entities = [SiloEntity::class, MovimientoEntity::class, SolicitudCamionEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun siloDao(): SiloDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun solicitudCamionDao(): SolicitudCamionDao

    companion object {
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        // Patrón Singleton: crea la base de datos una sola vez y la reutiliza en toda la app.
        // 'synchronized' evita que dos hilos la creen al mismo tiempo.
        fun obtenerBaseDatos(context: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "silos_ariztia_db"
                )
                    // Si cambia la versión, borra y recrea la base (solo para desarrollo; en producción
                    // se escribirían migraciones para no perder datos).
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}
