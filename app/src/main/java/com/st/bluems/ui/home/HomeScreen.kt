package com.st.bluems.ui.home

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.st.bluems.NFCConnectionViewModel
import com.st.bluems.ui.composable.ConnectionStatusDialog
import com.st.bluems.ui.composable.HomeScreenNavigation
import com.st.ui.composables.ComposableLifecycle
import com.st.user_profiling.StUserProfilingConfig
import com.st.user_profiling.model.LevelProficiency
import com.st.user_profiling.model.ProfileType

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
    nfcViewModel: NFCConnectionViewModel
) {

    val activity = LocalActivity.current
    val context = LocalContext.current

    var isBleEnabled by rememberSaveable{ mutableStateOf(false) }
    var isLocationEnabled by rememberSaveable { mutableStateOf(false) }

    var bleReceiver by remember { mutableStateOf<BroadcastReceiver?>(null) }
    var blePairingReceiver by remember { mutableStateOf<BroadcastReceiver?>(null) }

    val connectionStatus by homeViewModel.connectionStatus.collectAsStateWithLifecycle()
    val connectionBoardName by homeViewModel.boardName.collectAsStateWithLifecycle()

    val isPairingRequest by homeViewModel.isPairingRequest.collectAsStateWithLifecycle()


    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                bleReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED ||
                            intent?.action == BluetoothAdapter.ACTION_REQUEST_ENABLE
                        ) {
                            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                                BluetoothAdapter.STATE_OFF -> isBleEnabled = false
                                BluetoothAdapter.STATE_ON -> isBleEnabled = true
                            }
                        }
                    }
                }

                blePairingReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent != null) {
                            //Log.i("NFC","NFC Pairing action=${intent.action}")
                            when (intent.action) {
                                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> onBoundStateChange(
                                    context = context,
                                    intent = intent,
                                    nfcViewModel = nfcViewModel
                                )

                                BluetoothDevice.ACTION_PAIRING_REQUEST -> onPairingRequest(
                                    context = context,
                                    intent = intent,
                                    nfcViewModel = nfcViewModel,
                                    homeViewModel = homeViewModel
                                )
                            }
                        }
                    }
                }

                val bluetoothManager =
                    ContextCompat.getSystemService(context, BluetoothManager::class.java)
                val bleAdapter = bluetoothManager?.adapter

                isBleEnabled = bleAdapter?.isEnabled ?: false

                isLocationEnabled = isLocationEnable(context)

                activity?.let {
                    homeViewModel.initLoginManager(activity = it)
                }
            }
            Lifecycle.Event.ON_START -> {
                context.registerReceiver(
                    bleReceiver,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                )

                context.registerReceiver(
                    blePairingReceiver,
                    IntentFilter().apply {
                        addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
                        addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    }
                )

                homeViewModel.checkVersionBetaRelease()
                homeViewModel.checkDisableHiddenDemos()
                homeViewModel.checkServerForced()
                homeViewModel.checkBoardsCatalogPresence()

                // To Check...
                homeViewModel.checkLoginStatus()
                homeViewModel.checkProfileLevel()
                StUserProfilingConfig.onDone = { level: LevelProficiency, type: ProfileType ->
                    homeViewModel.profileShow(level = level, type = type)
                }
            }

            Lifecycle.Event.ON_STOP -> {
                context.unregisterReceiver(bleReceiver)
                context.unregisterReceiver(blePairingReceiver)
            }
            else -> Unit
        }
    }

    val bleRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult() // Use StartActivityForResult
    ) { _ ->
        // After returning from settings, check if BLE is actually enabled
        val bluetoothManager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
        isBleEnabled = bluetoothManager?.adapter?.isEnabled ?: false
    }

    val locationRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isLocationEnabled = isLocationEnable(context)
    }

    HomeScreenNavigation(
        modifier = modifier,
        viewModel = homeViewModel,
        nfcViewModel = nfcViewModel,
        isBleEnabled = isBleEnabled,
        onEnableBle = {
            bleRequestLauncher.launch(Intent(ACTION_BLUETOOTH_SETTINGS))
        },
        isLocationEnable = isLocationEnabled,
        onEnableLocation = {
            locationRequestLauncher.launch(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
        }
    )

    ConnectionStatusDialog(
        isPairingRequest = isPairingRequest,
        connectionStatus = connectionStatus,
        boardName = connectionBoardName
    )

}

fun onBoundStateChange(context: Context?, intent: Intent,
                       nfcViewModel: NFCConnectionViewModel) {
    val boundState = intent.getIntExtra(
        BluetoothDevice.EXTRA_BOND_STATE,
        BluetoothDevice.BOND_NONE
    )
    //android lollipop show 2 notification -> we delete the pin only when we have finish
    // to bound
    if (boundState != BluetoothDevice.BOND_BONDING) {
        nfcViewModel.setNFCPairingPin(null)  //the pairing is done -> remove the pin
    }
}


@SuppressLint("MissingPermission")
fun onPairingRequest(context: Context?, intent: Intent,nfcViewModel: NFCConnectionViewModel,homeViewModel: HomeViewModel) {

    val pairingPin = nfcViewModel.pairingPin.value
//    if(pairingPin!=null) {
//        Log.i("NFC", "NFC pairingPin=${String(pairingPin)}")
//    } else {
//        Log.i("NFC", "NFC pairingPin ... null")
//    }
    @Suppress("DEPRECATION") val device =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    //Log.i("NFC","NFC device=${device}")
    if ((device != null) && (pairingPin != null)) {
        val node = homeViewModel.getNodeFromNodeId(device.address)
        node?.device?.setPin(pairingPin)
//        node?.device?.let {
//            Log.i("NFC", "NFC setPin(pairingPin)")
//        }
        //nfcViewModel.setNFCPairingPin(null)
    } else if (device != null) {
        //Toast.makeText(context,"Pin 123456",Toast.LENGTH_SHORT).show()
        //Log.i("NFC", "NFC homeViewModel.setIsPairingRequest(true)")
        homeViewModel.setIsPairingRequest(true)
    }
}

private fun isLocationEnable(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(manager)
}
