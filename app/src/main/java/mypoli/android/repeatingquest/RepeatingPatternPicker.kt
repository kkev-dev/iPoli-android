package mypoli.android.repeatingquest

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.dialog_repeating_picker.view.*
import kotlinx.android.synthetic.main.view_dialog_header.view.*
import mypoli.android.R
import mypoli.android.common.view.BaseDialogController
import mypoli.android.repeatingquest.entity.RepeatingPattern


/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 2/16/18.
 */
class RepeatingPatternPicker : BaseDialogController {

    private lateinit var repeatingPattern: RepeatingPattern
    private lateinit var resultListener: (RepeatingPattern) -> Unit

    constructor(args: Bundle? = null) : super(args)

    constructor(
        repeatingPattern: RepeatingPattern? = null,
        resultListener: (RepeatingPattern) -> Unit
    ) : this() {
        this.repeatingPattern = repeatingPattern ?: RepeatingPattern.Daily
        this.resultListener = resultListener
    }

    override fun onCreateContentView(inflater: LayoutInflater, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_repeating_picker, null)
        val spinnerArrayAdapter =
            ArrayAdapter<String>(view.context, android.R.layout.simple_spinner_dropdown_item,
                (1..31).map { it.toString() })
        view.rqCount.adapter = spinnerArrayAdapter
        return view
    }

    override fun onCreateDialog(
        dialogBuilder: AlertDialog.Builder,
        contentView: View,
        savedViewState: Bundle?
    ): AlertDialog =
        dialogBuilder
            .setPositiveButton("OK", { _, _ ->
                resultListener(repeatingPattern)
            })
            .setNegativeButton(R.string.cancel, null)
            .create()

    override fun onHeaderViewCreated(headerView: View?) {
        headerView!!.dialogHeaderTitle.setText("Pick repeating pattern")
        headerView.dialogHeaderIcon.setImageResource(R.drawable.logo)
    }
}