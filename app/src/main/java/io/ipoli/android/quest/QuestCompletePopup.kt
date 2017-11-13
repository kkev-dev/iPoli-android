package io.ipoli.android.quest

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.view.LayoutInflater
import android.view.View
import io.ipoli.android.R
import io.ipoli.android.common.view.anim.TypewriterTextAnimator
import io.ipoli.android.common.view.visible
import io.ipoli.android.common.view.BasePopup
import kotlinx.android.synthetic.main.popup_quest_complete.view.*


class QuestCompletePopup(private val earnedXP: Int) : BasePopup() {

    override fun createView(inflater: LayoutInflater): View =
        inflater.inflate(R.layout.popup_quest_complete, null)

    override fun onEnterAnimationEnd(contentView: View) {
        startTypingAnimation(contentView)
    }

    private fun startTypingAnimation(contentView: View) {
        val title = contentView.title

        val typewriterAnim = TypewriterTextAnimator.of(title, "Quest Complete")
        typewriterAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                startEarnedRewardAnimation(contentView)
            }
        })
        typewriterAnim.start()
    }

    private fun startEarnedRewardAnimation(contentView: View) {
        val earnedXP = contentView.earnedXP
        earnedXP.text = "+ ${this.earnedXP}XP"
        earnedXP.visible = true

        val scaleX = ObjectAnimator.ofFloat(earnedXP, "scaleX", 1f, 1.6f, 1f)
        val scaleY = ObjectAnimator.ofFloat(earnedXP, "scaleY", 1f, 1.6f, 1f)
        val scaleAnimation = AnimatorSet()
        scaleAnimation.interpolator = LinearOutSlowInInterpolator()
        scaleAnimation.playTogether(scaleX, scaleY)

        scaleAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                contentView.postDelayed({
                    playExitAnimation(contentView)
                }, 2000)
            }
        })

        scaleAnimation.start()
    }
}