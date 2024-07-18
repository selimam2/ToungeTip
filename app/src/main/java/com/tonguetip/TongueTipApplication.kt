package com.tonguetip

import android.app.Application

class TongueTipApplication: Application(){
    override fun onCreate() {
        super.onCreate()
        DatabaseHandler.initDataBaseInst(this)
    }

}