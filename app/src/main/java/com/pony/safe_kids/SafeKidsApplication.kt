package com.pony.safe_kids

import android.app.Application

class SafeKidsApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
