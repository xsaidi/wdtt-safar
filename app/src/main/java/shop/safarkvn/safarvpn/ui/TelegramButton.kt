package shop.safarkvn.safarvpn.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import shop.safarkvn.safarvpn.R

const val SAFARVPN_BOT_URL = "https://t.me/safarvpn_bot"
const val SAFARVPN_BOT_HANDLE = "@safarvpn_bot"

@Composable
fun TelegramButton(
    label: String = "Купить/Продлить подписку",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Button(
        onClick = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(SAFARVPN_BOT_URL)).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                )
            } catch (_: Exception) {
                Toast.makeText(context, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_telegram),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
