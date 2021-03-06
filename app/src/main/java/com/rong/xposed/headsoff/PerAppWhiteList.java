package com.rong.xposed.headsoff;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.rong.lib.utils.HtmlString;
import com.rong.xposed.headsoff.adapter.PackageListItemDivider;
import com.rong.xposed.headsoff.adapter.WhiteListAdapter;
import com.rong.xposed.headsoff.utils.AppUtils;
import com.rong.xposed.headsoff.utils.SettingValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
   Copyright (c) 2016-2017, j2Rong     
     
   Licensed under the Apache License, Version 2.0 (the "License");     
   you may not use this file except in compliance with the License.     
   You may obtain a copy of the License at     
     
       http://www.apache.org/licenses/LICENSE-2.0     
     
   Unless required by applicable law or agreed to in writing, software     
   distributed under the License is distributed on an "AS IS" BASIS,     
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.     
   See the License for the specific language governing permissions and     
   limitations under the License.   
*/
public class PerAppWhiteList extends AppCompatActivity implements
		View.OnClickListener, WhiteListAdapter.OnRegexRemoveListener, TextView.OnEditorActionListener {

	private String packageName;
	private WhiteListAdapter mAdapter;
	private List<String> mList = null;
	private AppCompatEditText edtRegex;
	private AppCompatButton btnAdd;
	private AppCompatEditText testEdit;
	private TextView txtHint;

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppUtils.updateConfiguration(this);

		setContentView(R.layout.activity_per_app_white_list);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		setupActionBar();

		Intent intent = getIntent();
		Bundle param = intent.getExtras();
		if (param != null) {
			packageName = param.getString(SettingValues.KEY_PKG_NAME);
		}

		if (TextUtils.isEmpty(packageName)) {
			Toast.makeText(this, R.string.white_list_package_name_is_empty, Toast.LENGTH_LONG).show();
			finish();
		}

		txtHint = (TextView) findViewById(R.id.empty_hint);

		RecyclerView list = (RecyclerView) findViewById(R.id.regex_list);
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
		list.setLayoutManager(layoutManager);
		list.setItemAnimator(new DefaultItemAnimator());
		list.addItemDecoration(new PackageListItemDivider(this));

		mAdapter = new WhiteListAdapter(this, packageName, mList);
		mAdapter.setOnRegexRemoveListener(this);
		list.setAdapter(mAdapter);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mList = getWhiteList(packageName);
				mAdapter.setItemList(mList);
				mAdapter.notifyDataSetChanged();
				checkShowEmptyListHint();
			}
		}, 50);

		edtRegex = (AppCompatEditText) findViewById(R.id.edit_regex);
		edtRegex.setOnEditorActionListener(this);

		btnAdd = (AppCompatButton) findViewById(R.id.btn_add);
		btnAdd.setOnClickListener(this);

		final TextInputLayout testLayout = (TextInputLayout) findViewById(R.id.test_layout);
		testEdit = (AppCompatEditText) findViewById(R.id.test_regex);
		testEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String pattern = edtRegex.getText().toString();
				String test = testEdit.getText().toString();
				String defaultHint = getString(R.string.white_list_edit_test_hint);

				if (TextUtils.isEmpty(test)) {
					testLayout.setError(null);
					testLayout.setHint(defaultHint);
					return;
				}

				if (TextUtils.isEmpty(pattern)) {
					testLayout.setError(getString(R.string.white_list_regex_test_empty_pattern));
					testLayout.setHint(defaultHint);
					return;
				}

				if (!isRegexValid(pattern)) {
					testLayout.setError(getString(R.string.white_list_regex_test_invalid_pattern));
					testLayout.setHint(defaultHint);
					return;    // not a valid regex
				}

				//start check
				try {
					Pattern regex = Pattern.compile(pattern);
					Matcher matcher = regex.matcher(test);
					if (matcher.find()) {
						testLayout.setError(null);
						testLayout.setHint(getString(R.string.white_list_regex_test_match));
					} else {
						testLayout.setError(getString(R.string.white_list_regex_test_not_match));
						testLayout.setHint(defaultHint);
					}
				} catch (Exception e) {
					testLayout.setError(getString(R.string.white_list_regex_test_error_unknown));
					testLayout.setError(defaultHint);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});


	}

	private void checkShowEmptyListHint() {
		int count = mAdapter.getItemCount();
		if (count == 0) {
			txtHint.setVisibility(View.VISIBLE);
		} else {
			txtHint.setVisibility(View.GONE);
		}
	}

	private boolean checkIfExists(String pattern) {
		for (String str : mList) {
			if (str.equals(pattern))
				return true;
		}

		return false;
	}

	@Override
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.btn_add) {
			String pattern = edtRegex.getText().toString();
			boolean bClear = false;

			if (TextUtils.isEmpty(pattern)) {
				Toast.makeText(this, R.string.white_list_add_empty, Toast.LENGTH_LONG).show();
				return;
			}

			if (isRegexValid(pattern)) {
				if (mList.size() < SettingValues.WHITE_LIST_MAX) {
					if (checkIfExists(pattern)) {
						Toast.makeText(this, R.string.white_list_add_exists_already, Toast.LENGTH_LONG).show();
						return;
					}

					mList.add(pattern);
					bClear = true;

					SharedPreferences prefs = getSharedPreferences(SettingValues.PREF_FILE, Context.MODE_WORLD_READABLE);
					SharedPreferences.Editor editor = prefs.edit();
					String key = packageName + SettingValues.KEY_SUFFIX_WHITELIST_COUNT;
					editor.putInt(key, mList.size());
					editor.apply();

					key = packageName + SettingValues.KEY_SUFFIX_WHITELIST;
					editor.putStringSet(key, new HashSet<>(mList));
					editor.apply();

					mAdapter.notifyDataSetChanged();
					checkShowEmptyListHint();
					Toast.makeText(this, R.string.white_list_add_success, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this,
							getString(R.string.white_list_exceeded_limitation, SettingValues.WHITE_LIST_MAX),
							Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(this, R.string.white_list_invalid_regex_pattern, Toast.LENGTH_LONG).show();
				edtRegex.requestFocus();
			}

			if (bClear) {
				edtRegex.setText("");
				testEdit.setText("");

				if (testEdit.hasFocus())
					testEdit.clearFocus();
			}
		}
	}

	@Override
	public void onRegexRemove(int position) {
		checkShowEmptyListHint();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private boolean isRegexValid(String pattern) {
		try {
			Pattern.compile(pattern);
			return true;
		} catch (PatternSyntaxException pse) {
			return false;
		}
	}

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	private List<String> getWhiteList(String pkg) {
		SharedPreferences prefs = getSharedPreferences(SettingValues.PREF_FILE, Context.MODE_WORLD_READABLE);
		String key = pkg + SettingValues.KEY_SUFFIX_WHITELIST_COUNT;

		int count = prefs.getInt(key, SettingValues.DEFAULT_WHITELIST_COUNT);
		if (count == 0) {
			return new ArrayList<>();
		}

		key = pkg + SettingValues.KEY_SUFFIX_WHITELIST;
		Set<String> set = prefs.getStringSet(key, null);
		if (set == null)
			return new ArrayList<>();

		return new ArrayList<>(set);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_per_app_white_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == android.R.id.home) {
			NavUtils.navigateUpFromSameTask(this);
			return true;
		} else if (id == R.id.action_white_list_help) {
			showHelp();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		NavUtils.navigateUpFromSameTask(this);
	}

	private void showHelp() {
		HtmlString msg = new HtmlString();
		int titleColor = ContextCompat.getColor(this, R.color.colorPrimaryDark);

		String dot = getString(R.string.white_list_help_dot);

		msg.nl();
		msg.color(getString(R.string.white_list_help_matching_rules), titleColor);
		msg.append(" (").append(getString(R.string.white_list_exceeded_limitation, SettingValues.WHITE_LIST_MAX));
		msg.append(")").nl().nl();
		msg.bold(dot).blank(2).append(getString(R.string.white_list_help_not_filtered_if_matching_rules)).nl();
		msg.bold(dot).blank(2).append(getString(R.string.white_list_help_ignore_if_not_heads_up_originally)).nl().nl();

		msg.color(getString(R.string.white_list_help_matching_entries), titleColor).nl().nl();
		msg.bold(dot).blank(2).append(getString(R.string.white_list_help_content_title)).nl();
		msg.bold(dot).blank(2).append(getString(R.string.white_list_help_content_text)).nl();
		msg.bold(dot).blank(2).append(getString(R.string.white_list_help_content_info)).nl();
		msg.bold(dot).blank(2).append(getString(R.string.white_list_help_sub_text)).nl().nl();

		final AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_AlertDialogStyle)
				.setTitle(R.string.white_list_help_title)
				.setMessage(msg.fromHtml())
				.setPositiveButton(R.string.white_list_help_btn_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setNegativeButton(getString(R.string.white_list_help_btn_example), null)
				.create();
		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Notification n = new NotificationCompat.Builder(PerAppWhiteList.this)
						.setContentTitle(getString(R.string.white_list_help_content_title))
						.setContentText(getString(R.string.white_list_help_content_text))
						.setContentInfo(getString(R.string.white_list_help_content_info))
						.setSubText(getString(R.string.white_list_help_sub_text))
						.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
						.setSmallIcon(R.drawable.ic_notification)
						.build();
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nm.notify(1, n);
			}
		});
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		boolean handled = false;

		if(EditorInfo.IME_ACTION_DONE == actionId || EditorInfo.IME_ACTION_UNSPECIFIED == actionId)
		{
			onClick(btnAdd);
			handled = true;
		}

		return handled;
	}
}
