package com.rich.rallypacenotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.rich.rallypacenotes.map.BundledMapPackageProvisioner
import com.rich.rallypacenotes.map.LocalMapPackageImporter
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    private var mapPackageVersion by mutableIntStateOf(0)

    private val selectMapPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.openInputStream(uri)?.let { source ->
            runCatching { LocalMapPackageImporter.importInto(filesDir, source) }
                .onSuccess { mapPackageVersion++ }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext)
        BundledMapPackageProvisioner.provisionInto(this)
        setContent {
            ReplayAlphaApp(
                appFilesDir = filesDir,
                mapPackageVersion = mapPackageVersion,
                onImportMapPackage = { selectMapPackage.launch(arrayOf("application/octet-stream", "*/*")) },
            )
        }
    }
}
