package shop.safarkvn.safarvpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast

/** Requests VPN consent for entry points that do not have MainActivity available. */
class VpnPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent == null) {
            startTunnel()
        } else {
            @Suppress("DEPRECATION")
            startActivityForResult(permissionIntent, REQUEST_VPN_PERMISSION)
        }
    }

    @Deprecated("Replaced by Activity Result APIs; this activity has one system request.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VPN_PERMISSION) return

        if (resultCode == RESULT_OK && VpnService.prepare(this) == null) {
            startTunnel()
        } else {
            Toast.makeText(this, "VPN-разрешение не выдано", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startTunnel() {
        TunnelControl.startFromSavedSettings(applicationContext)
        finish()
    }

    private companion object {
        const val REQUEST_VPN_PERMISSION = 1
    }
}
