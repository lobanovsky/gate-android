package ru.housekpr.gate.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.my.target.ads.MyTargetView
import com.my.target.common.models.IAdLoadingError
import ru.housekpr.gate.BuildConfig

@Composable
internal fun VkBanner300x250(
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.VK_ADS_ENABLED) return

    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }
    val adView = remember {
        MyTargetView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setSlotId(BuildConfig.VK_ADS_BANNER_SLOT_ID.toInt())
            setAdSize(MyTargetView.AdSize.ADSIZE_300x250)
            setListener(object : MyTargetView.MyTargetViewListener {
                override fun onLoad(myTargetView: MyTargetView) {
                    isLoaded = true
                }

                override fun onNoAd(
                    adLoadingError: IAdLoadingError,
                    myTargetView: MyTargetView
                ) {
                    isLoaded = false
                }

                override fun onShow(myTargetView: MyTargetView) = Unit

                override fun onClick(myTargetView: MyTargetView) = Unit
            })
            load()
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    if (!isLoaded) return

    AndroidView(
        modifier = modifier,
        factory = { adView }
    )
}
