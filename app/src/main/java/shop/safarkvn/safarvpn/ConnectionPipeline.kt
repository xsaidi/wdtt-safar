package shop.safarkvn.safarvpn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ConnectionStep(val order: Int, val label: String) {
    DNS(0, "DNS"),
    VK(1, "VK"),
    CAPTCHA(2, "Капча"),
    WRAP(3, "WRAP"),
    TURN(4, "TURN"),
    DTLS(5, "DTLS"),
    WORKERS(6, "Потоки"),
    VPN(7, "VPN"),
    DONE(8, "Готово"),
}

data class ConnectionPipelineState(
    val current: ConnectionStep? = null,
    val completed: Set<ConnectionStep> = emptySet(),
    val failed: ConnectionStep? = null,
    val timedOut: Boolean = false,
    val captchaRequired: Boolean = false,
    val visible: Boolean = false,
    val timeoutSec: Int = 0,
) {
    fun stepsToShow(): List<ConnectionStep> = buildList {
        add(ConnectionStep.DNS)
        add(ConnectionStep.VK)
        if (captchaRequired) add(ConnectionStep.CAPTCHA)
        add(ConnectionStep.WRAP)
        add(ConnectionStep.TURN)
        add(ConnectionStep.DTLS)
        add(ConnectionStep.WORKERS)
        add(ConnectionStep.VPN)
    }
}

@Composable
fun ConnectionPipelineCard(state: ConnectionPipelineState, isDark: Boolean) {
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.56f)
    }

    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (state.failed != null) "Подключение не завершено" else "Подключение",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            state.stepsToShow().forEach { step ->
                val status = when {
                    state.failed == step -> if (state.timedOut) "таймаут" else "ошибка"
                    step in state.completed -> "готово"
                    state.current == step -> "выполняется"
                    else -> "ожидание"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(step.label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (status) {
                            "ошибка", "таймаут" -> MaterialTheme.colorScheme.error
                            "готово" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
