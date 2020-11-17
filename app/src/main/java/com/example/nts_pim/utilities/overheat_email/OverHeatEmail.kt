package com.example.nts_pim.utilities.overheat_email


import android.annotation.SuppressLint
import io.reactivex.Completable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


object OverHeatEmail {

    @SuppressLint("CheckResult")
    fun sendMail(vehicleNumber: String, pimStartTime: String, overheatTimeStamp: String): Completable {
        val timeInBetween = timeBetweenStartAndOverheat(pimStartTime, overheatTimeStamp)
        val ntsEmail = "nts@rideyellow.com"
        val password = "NTsGoo1!"
        val toEmail = "shan@rideyellow.com"
        val subject = "PIM Overheating detected - $vehicleNumber"
        val message = "Started: $pimStartTime,\n Overheat detected: $overheatTimeStamp, \n Time between: $timeInBetween"

        return Completable.create { emitter ->

            //configure SMTP server
            val props: Properties = Properties().also {
                it.put("mail.smtp.host", "smtp.gmail.com")
                it.put("mail.smtp.socketFactory.port", "465")
                it.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                it.put("mail.smtp.auth", "true")
                it.put("mail.smtp.port", "465")
            }

            //Creating a session
            val session = Session.getDefaultInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(ntsEmail, password)
                }
            })

            try {

                MimeMessage(session).let { mime ->
                    mime.setFrom(InternetAddress(ntsEmail))
                    //Adding receiver
                    mime.addRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                    mime.addRecipient(Message.RecipientType.CC, InternetAddress( "josh@rideyellow.com"))
                    mime.addRecipient(Message.RecipientType.CC, InternetAddress("Wamiq@rideyellow.com"))
                    mime.addRecipient(Message.RecipientType.CC, InternetAddress("hayden@nts.taxi"))
                    //Adding subject
                    mime.subject = subject
                    //Adding message
                    mime.setText(message)
                    //send mail
                    Transport.send(mime)
                }

            } catch (e: MessagingException) {
                emitter.onError(e)
            }

            //ending subscription
            emitter.onComplete()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timeBetweenStartAndOverheat(startTime: String, overHeatTime: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        try {
            val sDate: Date? = format.parse(startTime)
            val oDate: Date? = format.parse(overHeatTime)
            return timeDifference(sDate, oDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return ""
    }
    private fun timeDifference(startDate: Date?, endDate: Date?):String {
        //milliseconds
        var different = startDate?.time?.let { endDate?.time?.minus(it) }
        val secondsInMilli: Long = 1000
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24
        val elapsedDays = different?.div(daysInMilli)
        different = different?.rem(daysInMilli)
        val elapsedHours = different?.div(hoursInMilli)
        different = different?.rem(hoursInMilli)
        val elapsedMinutes = different?.div(minutesInMilli)
        different = different?.rem(minutesInMilli)
        val elapsedSeconds = different?.div(secondsInMilli)
        return "$elapsedDays days, $elapsedHours hours, $elapsedMinutes min, $elapsedSeconds seconds"
    }

}