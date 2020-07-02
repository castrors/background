package com.example.background

import android.annotation.TargetApi
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import io.flutter.view.FlutterMain
import java.util.*


@RequiresApi(api = Build.VERSION_CODES.O)
class CustomAutofillService : AutofillService() {

    override fun onFillRequest(
            request: FillRequest,
            cancellationSignal: CancellationSignal,
            callback: FillCallback
    ) {

        // Get the structure from the request
        val context: List<FillContext> = request.fillContexts
        val structure: AssistStructure = context[context.size - 1].structure

        // Traverse the structure looking for nodes to fill out.
        val parsedStructure: ParsedStructure = parseStructure(structure)
        val sharedPref = application.applicationContext.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);

        // Fetch user data that matches the fields.
        val username = sharedPref.getString("flutter.DUMMY_USERNAME", "fallback@email.com")
        val password = sharedPref.getString("flutter.DUMMY_PASSWORD", "fallback")

        Log.w("CUSTOM_AUTOFILL_SERVICE", "onFillRequest")
        BackgroundListener.onInitialized(this)
        BackgroundListener.sendBackgroundMessageToExecute(this, "hello there!@#!@#!@", null)

        // Build the presentation of the datasets
        val usernamePresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
        usernamePresentation.setTextViewText(android.R.id.text1, "Username")
        val passwordPresentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
        passwordPresentation.setTextViewText(android.R.id.text1, "Password")

        // Add a dataset to the response
        val fillResponse: FillResponse = FillResponse.Builder()
                .addDataset(Dataset.Builder()
                        .setValue(
                                parsedStructure.usernameId,
                                AutofillValue.forText(username),
                                usernamePresentation
                        )
                        .setValue(
                                parsedStructure.passwordId,
                                AutofillValue.forText(password),
                                passwordPresentation
                        )
                        .build())
                .build()

        // If there are no errors, call onSuccess() and pass the response
        callback.onSuccess(fillResponse)
    }

    private fun parseStructure(structure: AssistStructure): ParsedStructure {
        val fields: MutableMap<String, AutofillId> = mutableMapOf()
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            addAutofillableFields(fields, node)
        }
        return ParsedStructure(fields.values.first(), fields.values.last())

    }

    private fun addAutofillableFields(fields: MutableMap<String, AutofillId>,
                                      node: ViewNode) {
        val hints = node.autofillHints
        if (hints != null) {
            // We're simple, we only care about the first hint
            val hint = hints[0].toLowerCase()
            if (hint != null) {
                val id = node.autofillId
                if (!fields.containsKey(hint)) {

                    fields[hint] = id
                } else {

                }
            }
        }
        val childrenSize = node.childCount
        for (i in 0 until childrenSize) {
            addAutofillableFields(fields, node.getChildAt(i))
        }
    }


    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onFailure("Not implemented")
    }


}

data class ParsedStructure(var usernameId: AutofillId, var passwordId: AutofillId)

data class UserData(var username: String, var password: String)

/**
 * This is a class containing helper methods for building Autofill Datasets and Responses.
 */
object RemoteViewsHelper {

    fun viewsWithAuth(packageName: String, text: String): RemoteViews {
        return simpleRemoteViews(packageName, text, R.drawable.ic_lock_black_24dp)
    }

    fun viewsWithNoAuth(packageName: String, text: String): RemoteViews {
        return simpleRemoteViews(packageName, text, R.drawable.ic_person_black_24dp)
    }

    private fun simpleRemoteViews(
            packageName: String, remoteViewsText: String,
            @DrawableRes drawableId: Int
    ): RemoteViews {
        val presentation = RemoteViews(
                packageName,
                R.layout.multidataset_service_list_item
        )
        presentation.setTextViewText(R.id.text, remoteViewsText)
        presentation.setImageViewResource(R.id.icon, drawableId)
        return presentation
    }
}

data class AutofillHeuristic(
        val weight: Int,
        val predicate: AssistStructure.ViewNode.(node: AssistStructure.ViewNode) -> Boolean
)

private fun MutableList<AutofillHeuristic>.heuristic(
        weight: Int,
        predicate: AssistStructure.ViewNode.(node: AssistStructure.ViewNode) -> Boolean
) =
        add(AutofillHeuristic(weight, predicate))


@TargetApi(Build.VERSION_CODES.O)
private fun MutableList<AutofillHeuristic>.autofillHint(weight: Int, hint: String) =
        heuristic(weight) { autofillHints?.contains(hint) == true }

@TargetApi(Build.VERSION_CODES.O)
private fun MutableList<AutofillHeuristic>.idEntry(weight: Int, match: String) =
        heuristic(weight) { idEntry == match }

@TargetApi(Build.VERSION_CODES.O)
private fun MutableList<AutofillHeuristic>.htmlAttribute(weight: Int, attr: String, value: String) =
        heuristic(weight) { htmlInfo?.attributes?.firstOrNull { it.first == attr && it.second == value } != null }

@TargetApi(Build.VERSION_CODES.O)
private fun MutableList<AutofillHeuristic>.defaults(hint: String, match: String) {
    autofillHint(900, hint)
    idEntry(800, match)
    heuristic(700) { idEntry?.toLowerCase(Locale.ROOT)?.contains("user") == true }
}

@TargetApi(Build.VERSION_CODES.O)
enum class AutofillInputType(val heuristics: List<AutofillHeuristic>) {
    Email(mutableListOf<AutofillHeuristic>().apply {
        defaults(View.AUTOFILL_HINT_EMAIL_ADDRESS, "email")
        htmlAttribute(400, "type", "email")
        htmlAttribute(300, "name", "email")
        heuristic(200) { hint?.toLowerCase(java.util.Locale.ROOT)?.contains("mail") == true }
    }),
    UserName(mutableListOf<AutofillHeuristic>().apply {
        defaults(View.AUTOFILL_HINT_USERNAME, "user")
        htmlAttribute(400, "name", "user")
        htmlAttribute(400, "name", "username")
    }),
    Password(mutableListOf<AutofillHeuristic>().apply {
        defaults(View.AUTOFILL_HINT_PASSWORD, "password")
        htmlAttribute(400, "type", "password")
        heuristic(500) { inputType.hasFlag(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) }
        heuristic(499) { inputType.hasFlag(android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) }
        heuristic(498) { inputType.hasFlag(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) }
    }),
}


inline fun Int?.hasFlag(flag: Int) = this != null && flag and this == flag
inline fun Int.withFlag(flag: Int) = this or flag
inline fun Int.minusFlag(flag: Int) = this and flag.inv()


data class MatchedField(val heuristic: AutofillHeuristic, val autofillId: AutofillId)

