package mypoli.android

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.github.moduth.blockcanary.BlockCanary
import com.github.moduth.blockcanary.BlockCanaryContext
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import io.fabric.sdk.android.Fabric
import mypoli.android.common.di.*
import mypoli.android.common.job.myPoliJobCreator
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import space.traversal.kapsule.transitive
import timber.log.Timber
import java.util.*


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 7/7/17.
 */

class myPoliApp : Application() {

    private lateinit var module: Module

    companion object {
        lateinit var refWatcher: RefWatcher

        lateinit var instance: myPoliApp

        fun module(context: Context) =
            (context.applicationContext as myPoliApp).module
    }

    val INSTANCE_PROJECTION = arrayOf(
        Instances.EVENT_ID, // 0
        Instances.BEGIN, // 1
        Instances.END, // 1
        Instances.START_MINUTE, // 1
        Instances.END_MINUTE, // 1
        Instances.TITLE,          // 2
        Instances.EVENT_LOCATION,
        Instances.DURATION,
        Instances.CALENDAR_TIME_ZONE
    )

    // The indices for the projection array above.
    private val PROJECTION_ID_INDEX = 0
    private val PROJECTION_BEGIN_INDEX = 1
    private val PROJECTION_END_INDEX = 2
    private val PROJECTION_START_MIN_INDEX = 3
    private val PROJECTION_END_MIN_INDEX = 4
    private val PROJECTION_TITLE_INDEX = 5
    private val PROJECTION_LOCATION_INDEX = 6
    private val PROJECTION_DURATION_INDEX = 7
    private val PROJECTION_TIME_ZONE_INDEX = 8

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()


        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        AndroidThreeTen.init(this)
        // Initialize Realm. Should only be done once when the application starts.
//        Realm.init(this)
//        val db = Database()
        Timber.plant(Timber.DebugTree())


//        val p = CalendarProvider(this)
//        val c = p.getCalendar(3)

        val beginTime = Calendar.getInstance()
        beginTime.set(2018, 1, 2, 0, 0, 0)

        val endTime = Calendar.getInstance()
        endTime.set(2018, 1, 2, 23, 59, 59)

        val cr = contentResolver

        val builder = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, beginTime.timeInMillis)
        ContentUris.appendId(builder, endTime.timeInMillis)

        val selection = (CalendarContract.Events.CALENDAR_ID + " = ?")
        val selectionArgs = arrayOf("3")

        val cur = cr.query(
            builder.build(),
            INSTANCE_PROJECTION,
            selection,
            selectionArgs,
            null
        )

        while (cur.moveToNext()) {
            val eventID = cur.getLong(PROJECTION_ID_INDEX);
            val beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
            val endVal = cur.getLong(PROJECTION_END_INDEX);
            val startMin = cur.getLong(PROJECTION_START_MIN_INDEX);
            val endMin = cur.getLong(PROJECTION_END_MIN_INDEX);
            val title = cur.getString(PROJECTION_TITLE_INDEX)
            val loc = cur.getString(PROJECTION_LOCATION_INDEX)
            val dur = cur.getString(PROJECTION_DURATION_INDEX)
            val tz = cur.getString(PROJECTION_TIME_ZONE_INDEX)
            Timber.d("AAA $eventID ${Instant.ofEpochMilli(beginVal).atZone(ZoneId.of(tz))} $endVal $startMin $endMin $title $loc $dur $tz")
        }

        cur.close()

//        val instances = p.getInstances(beginTime.timeInMillis, endTime.timeInMillis).list
//        Timber.d("AAA ${instances.size}")
//        val i = instances.first()
//        Timber.d("AAA $i")

//        Logger.addLogAdapter(AndroidLogAdapter())

//        Timber.plant(object : Timber.DebugTree() {
//            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
//                Logger.log(priority, tag, message, t)
//            }
//        })

        if (!BuildConfig.DEBUG) {

            BlockCanary.install(this, object : BlockCanaryContext() {
                override fun provideBlockThreshold(): Int {
                    return 500
                }
            }).start()

            Fabric.with(
                Fabric.Builder(this)
                    .kits(Crashlytics())
                    .debuggable(BuildConfig.DEBUG)
                    .build()
            )

            refWatcher = LeakCanary.install(this)
        }

        JobManager.create(this).addJobCreator(myPoliJobCreator())

        val currentUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler({ thread, exception ->
            Log.println(Log.ERROR, thread.name, Log.getStackTraceString(exception))
            currentUncaughtExceptionHandler.uncaughtException(thread, exception)
        })


        module = Module(
            androidModule = MainAndroidModule(this),
            repositoryModule = CouchbaseRepositoryModule(),
            useCaseModule = MainUseCaseModule(),
            presenterModule = AndroidPresenterModule(),
            stateStoreModule = AndroidStateStoreModule()
        ).transitive()

        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                importance
            )
            channel.description = "Reminder notifications"
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }

//        TinyDancer.create().show(this)
    }
}