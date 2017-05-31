package io.ipoli.android.app.tutorial.fragments;

import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.ipoli.android.R;
import io.ipoli.android.app.ui.TypewriterView;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 5/31/17.
 */
public class IntroFragment extends Fragment {

    @BindView(R.id.tutorial_text)
    TypewriterView tutorialText;

    @BindView(R.id.tutorial_answer_positive)
    Button positiveAnswer;

    @BindView(R.id.tutorial_answer_negative)
    Button negativeAnswer;

    private View.OnClickListener showTipsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private View.OnClickListener showBackStoryListener = v -> {
        prepareForNextState();
        tutorialText.type("Many millennia ago, a daemon from another dimension came to conquer us. Yes, it used backdoor that was left intentionally open!")
                .pause(2000)
                .type(" His objective? Eat all the ice-cream on Earth! And kill the ONE!").pause().run(new Runnable() {
            @Override
            public void run() {
                tutorialText.setText("");
            }
        }).type("Counterintuitively enough, only the ONE can stop him! Let's hope that's you!")
                .type(" That deamon I was telling you about is pretty evil! He is so evil that his name is EVIL Snail! Yes, that name was given to him by his mother").run(new Runnable() {
            @Override
            public void run() {
                positiveAnswer.setText("Show tips");
                fadeIn(positiveAnswer);
                positiveAnswer.setOnClickListener(showTipsListener);
            }
        });
    };

    private View.OnClickListener skipChallengeListener = v -> {
        prepareForNextState();
        tutorialText.type("Don't worry! I believe in you!").pause().run(new Runnable() {
            @Override
            public void run() {
                acceptChallengeListener.onClick(v);
            }
        });
    };

    private View.OnClickListener acceptChallengeListener = v -> {
        prepareForNextState();
        tutorialText.type("So, we've established the fact that you might be the ONE. Would you like some tips?").run(new Runnable() {
            @Override
            public void run() {
                positiveAnswer.setText("Show tips");
                negativeAnswer.setText("The ONE?");
                fadeIn(positiveAnswer);
                fadeIn(negativeAnswer);

                negativeAnswer.setOnClickListener(showBackStoryListener);
                positiveAnswer.setOnClickListener(showTipsListener);
            }
        });
    };

    private View.OnClickListener improveListener = v -> {
        prepareForNextState();
        tutorialText.type("Gosh, we have another one of those...").pause()
                .delete("Gosh, we have another one of those...").pause()
                .type("Ok, Ok. Let's start over!").pause().run(() -> {
            tutorialText.setText("");
        }).type("Welcome, Hero!").pause().delete("Welcome, Hero!")
                .type("Your greatest journey is starting").pause().type(" today!").pause().delete("Your greatest journey is starting today!")
                .type("Are you ready to embrace your destiny of many hours of studying, working late, doing sports and eating yucky food?").run(() -> {
            positiveAnswer.setText("I Accept");
            negativeAnswer.setText("Nah, not me");
            fadeIn(positiveAnswer);
            fadeIn(negativeAnswer);
            positiveAnswer.setOnClickListener(acceptChallengeListener);
            negativeAnswer.setOnClickListener(skipChallengeListener);
        });
    };

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tutorial_intro, container, false);
        unbinder = ButterKnife.bind(this, v);

        fadeIn(v.findViewById(R.id.tutorial_logo), android.R.integer.config_longAnimTime, 1000);

        tutorialText.pause(2000).type("Psst, why are you here?").run(() -> {
            positiveAnswer.setText("To improve myself");
            negativeAnswer.setText("No idea");
            positiveAnswer.setOnClickListener(improveListener);
            fadeIn(positiveAnswer);
            fadeIn(negativeAnswer);
        });
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void fadeIn(View view, @IntegerRes int duration, long startDelay) {
        view.animate().alpha(1f).setStartDelay(startDelay).setDuration(getResources().getInteger(duration)).start();
    }

    private void fadeIn(View view) {
        fadeIn(view, android.R.integer.config_mediumAnimTime, 0);
    }

    private void fadeOut(View view) {
        view.animate().alpha(0f).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime)).start();
    }

    private void prepareForNextState() {
        fadeOut(positiveAnswer);
        fadeOut(negativeAnswer);
        tutorialText.setText("");
    }
}
