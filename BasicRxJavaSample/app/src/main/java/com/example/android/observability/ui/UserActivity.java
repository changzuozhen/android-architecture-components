/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.observability.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.android.observability.Injection;
import com.example.android.persistence.R;

import java.util.concurrent.Callable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


/**
 * Main screen of the app. Displays a user name and gives the option to update the user name.
 */
public class UserActivity extends AppCompatActivity {

    private static final String TAG = UserActivity.class.getSimpleName();

    private TextView mUserName;

    private EditText mUserNameInput;

    private Button mUpdateButton;

    private ViewModelFactory mViewModelFactory;

    private UserViewModel mViewModel;

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private TextView mUserList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        mUserName = findViewById(R.id.user_name);
        mUserList = findViewById(R.id.user_list);
        mUserNameInput = findViewById(R.id.user_name_input);
        mUpdateButton = findViewById(R.id.update_user);

        mViewModelFactory = Injection.provideViewModelFactory(this);
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(UserViewModel.class);
        mUpdateButton.setOnClickListener(v -> updateUserName());

        findViewById(R.id.add_user).setOnClickListener(v -> {
            String userName = mUserNameInput.getText().toString();
            mDisposable.add(mViewModel.insertUserName(userName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                                Log.d(TAG, "⚠️add_user success");
                            },
                            throwable -> Log.e(TAG, "⚠️Unable to ️add_user ", throwable)));

        });
        findViewById(R.id.list_user).setOnClickListener(v -> {
            long time = System.currentTimeMillis();
            mViewModel.getAllUserName()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .take(1)
                    .subscribe((users) -> {
                                Log.d(TAG, "⚠️list_user success " + users + time);
                            },
                            throwable -> {
                                Log.e(TAG, "⚠️Unable to list_user", throwable);
                            });
        });

        findViewById(R.id.delete_user).setOnClickListener(v -> {

            mDisposable.add(
                    Completable.fromCallable((Callable<Integer>) () -> mViewModel.deleteAllUsers())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> Log.d(TAG, "⚠️delete_user success rows: "),
                                    throwable -> Log.e(TAG, "⚠️Unable to ️delete_user", throwable)));
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Subscribe to the emissions of the user name from the view model.
        // Update the user name text view, at every onNext emission.
        // In case of error, log the exception.
        mDisposable.add(mViewModel.getUserName()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userName -> {
                            Log.d(TAG, "⚠️mUserName.setText " + userName);
                            mUserName.setText(userName);
                        },
                        throwable -> Log.e(TAG, "⚠️Unable to mUserName.setText ", throwable)));
        mDisposable.add(mViewModel.getAllUserName()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userName -> {
                            Log.d(TAG, "⚠️mUserList.setText");
                            mUserList.setText(userName);
                        },
                        throwable -> Log.e(TAG, "⚠️Unable to ️mUserList.setText", throwable)));
    }

    @Override
    protected void onStop() {
        super.onStop();

        // clear all the subscriptions
        mDisposable.clear();
    }

    private void updateUserName() {
        String userName = mUserNameInput.getText().toString();
        // Disable the update button until the user name update has been done
        mUpdateButton.setEnabled(false);
        // Subscribe to updating the user name.
        // Re-enable the button once the user name has been updated
        mDisposable.add(mViewModel.updateUserName(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> mUpdateButton.setEnabled(true),
                        throwable -> Log.e(TAG, "⚠️Unable to update username", throwable)));
    }


}
