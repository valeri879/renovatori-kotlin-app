package ge.renovatori.app.info

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ge.renovatori.app.R
import ge.renovatori.app.ui.CompanyInfo
import ge.renovatori.app.ui.PageTitle

/** Contact page (web `/contact`): working hours, phone, e-mail and social networks. */
@Composable
fun ContactScreen() {
    val uriHandler = LocalUriHandler.current

    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        PageTitle("კონტაქტი", Modifier.padding(horizontal = 16.dp))
        Text(
            "კომუნიკაცია ჩვენთან მარტივია — უბრალოდ აირჩიეთ ქვემოთ ჩამოთვლილი საშუალებებიდან ერთი.",
            modifier = Modifier.padding(16.dp)
        )
        ContactRow("სამუშაო საათები:", "ორშაბათი - პარასკევი, ${CompanyInfo.WORKING_HOURS}", R.drawable.ic_clock, onClick = null)
        HorizontalDivider()
        ContactRow("ტელეფონი:", CompanyInfo.PHONE_DISPLAY, R.drawable.ic_phone) { uriHandler.openUri(CompanyInfo.PHONE_URI) }
        HorizontalDivider()
        ContactRow("ელ-ფოსტა:", CompanyInfo.EMAIL, R.drawable.ic_envelope) { uriHandler.openUri(CompanyInfo.EMAIL_URI) }
        HorizontalDivider()
        Text(
            "სოციალური ქსელები:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )
        ContactRow(null, "Facebook", R.drawable.ic_facebook) { uriHandler.openUri(CompanyInfo.FACEBOOK_URL) }
        ContactRow(null, "Instagram", R.drawable.ic_instagram) { uriHandler.openUri(CompanyInfo.INSTAGRAM_URL) }
        ContactRow(null, "TikTok", R.drawable.ic_tiktok) { uriHandler.openUri(CompanyInfo.TIKTOK_URL) }
    }
}

@Composable
private fun ContactRow(label: String?, value: String, @DrawableRes icon: Int, onClick: (() -> Unit)?) {
    ListItem(
        overlineContent = label?.let { { Text(it) } },
        headlineContent = {
            // Actionable values use the link color, like the web anchors.
            Text(value, color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        },
        leadingContent = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}
