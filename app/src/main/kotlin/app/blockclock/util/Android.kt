package app.blockclock.util

import android.os.Build.VERSION.SDK_INT
import androidx.annotation.ChecksSdkIntAtLeast
import android.os.Build.VERSION_CODES as Sdk

object Android {

    @ChecksSdkIntAtLeast(api = Sdk.N_MR1)
    val N1 = SDK_INT >= Sdk.N_MR1 // 25
    @ChecksSdkIntAtLeast(api = Sdk.O)
    val O = SDK_INT >= Sdk.O // 26
    @ChecksSdkIntAtLeast(api = Sdk.O_MR1)
    val O1 = SDK_INT >= Sdk.O_MR1 // 27
    @ChecksSdkIntAtLeast(api = Sdk.P)
    val P = SDK_INT >= Sdk.P // 28
    @ChecksSdkIntAtLeast(api = Sdk.Q)
    val Q = SDK_INT >= Sdk.Q // 29
    @ChecksSdkIntAtLeast(api = Sdk.R)
    val R = SDK_INT >= Sdk.R // 30
    @ChecksSdkIntAtLeast(api = Sdk.S)
    val S = SDK_INT >= Sdk.S // 31
    @ChecksSdkIntAtLeast(api = Sdk.S_V2)
    val S2 = SDK_INT >= Sdk.S_V2 // 32
    @ChecksSdkIntAtLeast(api = Sdk.TIRAMISU)
    val T = SDK_INT >= Sdk.TIRAMISU // 33
    @ChecksSdkIntAtLeast(api = Sdk.UPSIDE_DOWN_CAKE)
    val U = SDK_INT >= Sdk.UPSIDE_DOWN_CAKE // 34
    @ChecksSdkIntAtLeast(api = Sdk.VANILLA_ICE_CREAM)
    val V = SDK_INT >= Sdk.VANILLA_ICE_CREAM // 35

    object Below {
        val O = !Android.O
        val P = !Android.P
        val Q = !Android.Q
        val R = !Android.R
        val T = !Android.T
    }
}