package com.milad.pricealarm

import android.app.Application
import com.milad.pricealarm.notification.NotificationHelper

class PriceAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
