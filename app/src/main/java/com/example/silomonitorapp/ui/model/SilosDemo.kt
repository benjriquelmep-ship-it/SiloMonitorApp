package com.example.silomonitorapp.ui.model

/**
 * Datos de ejemplo SOLO para los @Preview de Compose.
 * La app real obtiene los silos desde Room a través de SiloViewModel.
 */
val silosDemo = listOf(
    SiloUi("S-001", "SIL-001", "Silo Norte A", "Granja El Paico", "Galpón 1",
        30000.0, 21500.0, 3200.0, -33.7012, -71.0034),
    SiloUi("S-002", "SIL-002", "Silo Norte B", "Granja El Paico", "Galpón 2",
        30000.0, 9800.0, 3000.0, -33.7031, -71.0071),
    SiloUi("S-003", "SIL-003", "Silo Sur A", "Granja El Paico", "Galpón 3",
        25000.0, 3900.0, 2800.0, -33.7058, -71.0010),
    SiloUi("S-004", "SIL-004", "Silo Principal", "Granja Pomaire", "Galpón 1",
        40000.0, 31000.0, 4100.0, -33.6489, -71.1612),
    SiloUi("S-005", "SIL-005", "Silo Auxiliar", "Granja Pomaire", "Galpón 2",
        20000.0, 3500.0, 2500.0, -33.6512, -71.1655),
    SiloUi("S-006", "SIL-006", "Silo Central", "Granja Melipilla", "Galpón 4",
        35000.0, 12600.0, 3600.0, -33.6875, -71.2148),
)
