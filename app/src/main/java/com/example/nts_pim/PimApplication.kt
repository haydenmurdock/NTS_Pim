package com.example.nts_pim

import android.app.Application
import com.example.nts_pim.data.repository.trip_repository.TripRepository
import com.example.nts_pim.data.repository.trip_repository.TripRepositoryImpl
import com.example.nts_pim.fragments_viewmodel.check_vehicle_info.CheckVehicleInfoModelFactory
import com.example.nts_pim.fragments_viewmodel.email_or_text.EmailOrTextViewModelFactory
import com.example.nts_pim.fragments_viewmodel.interaction_complete.InteractionCompleteViewModelFactory
import com.example.nts_pim.fragments_viewmodel.live_meter.LiveMeterViewModelFactory
import com.example.nts_pim.fragments_viewmodel.receipt_information.ReceiptInformationViewModelFactory
import com.example.nts_pim.fragments_viewmodel.taxi_number.TaxiNumberViewModelFactory
import com.example.nts_pim.fragments_viewmodel.trip_review.TripReviewViewModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_settings_detail.VehicleSettingsDetailViewModelFactory
import com.example.nts_pim.fragments_viewmodel.vehicle_setup.VehicleSetupModelFactory
import com.example.nts_pim.fragments_viewmodel.welcome.WelcomeViewModelFactory
import com.example.nts_pim.utilities.LifeCycleCallBacks
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.sdk.reader.ReaderSdk
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

class PimApplication : Application(), KodeinAware{
    override val kodein = Kodein.lazy {
        import(androidXModule(this@PimApplication))

        //this connects the repo and the interface together

        bind<TripRepository>() with singleton {
            TripRepositoryImpl(
                instance()
            )
        }
        // This connects the View Model factories to the repo.
        bind() from provider { WelcomeViewModelFactory(instance()) }
        bind() from provider { VehicleSettingsDetailViewModelFactory(instance()) }
        bind() from provider { CheckVehicleInfoModelFactory(instance()) }
        bind() from provider { LiveMeterViewModelFactory(instance()) }
        bind() from provider { TaxiNumberViewModelFactory(instance()) }
        bind() from provider { VehicleSetupModelFactory(instance()) }
        bind() from provider { TripReviewViewModelFactory(instance()) }
        bind() from provider { EmailOrTextViewModelFactory(instance()) }
        bind() from provider { ReceiptInformationViewModelFactory(instance()) }
        bind() from provider { InteractionCompleteViewModelFactory(instance()) }
        }

        override fun onCreate() {
            super.onCreate()
            ReaderSdk.initialize(this)
            AndroidThreeTen.init(this)

            registerActivityLifecycleCallbacks(LifeCycleCallBacks())

        }
    }
