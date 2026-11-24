package io.github.simon1uo.autopilot

import android.app.Application
import io.github.simon1uo.autopilot.data.TaskRepository

class AutoPilotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: AutoPilotApp
            private set
        fun repo() = TaskRepository(instance)
    }
}
