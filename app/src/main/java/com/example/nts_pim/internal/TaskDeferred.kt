package com.example.nts_pim.internal

import kotlinx.coroutines.Deferred
import com.google.android.gms.tasks.Task

fun <T> Task<T>.asDeferred(): Deferred<T> {
    val deferred = kotlinx.coroutines.CompletableDeferred<T>()

    this.addOnSuccessListener { result ->
        deferred.complete(result)
    }

    this.addOnFailureListener { exception ->
        deferred.completeExceptionally(exception)
    }

    return deferred
}