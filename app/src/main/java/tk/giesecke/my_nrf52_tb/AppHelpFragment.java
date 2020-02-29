/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tk.giesecke.my_nrf52_tb;

import android.app.Dialog;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import no.nordicsemi.android.dfu.BuildConfig;

public class AppHelpFragment extends DialogFragment {
	private static final String ARG_TEXT = "ARG_TEXT";
	private static final String ARG_VERSION = "ARG_VERSION";

	public static AppHelpFragment getInstance(final int aboutResId, final boolean appendVersion) {
		final AppHelpFragment fragment = new AppHelpFragment();

		final Bundle args = new Bundle();
		args.putInt(ARG_TEXT, aboutResId);
		args.putBoolean(ARG_VERSION, appendVersion);
		fragment.setArguments(args);

		return fragment;
	}

	public static AppHelpFragment getInstance(final int aboutResId) {
		final AppHelpFragment fragment = new AppHelpFragment();

		final Bundle args = new Bundle();
		args.putInt(ARG_TEXT, aboutResId);
		args.putBoolean(ARG_VERSION, false);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
    @NonNull
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = getArguments();
		final StringBuilder text = new StringBuilder(getString(args.getInt(ARG_TEXT)));

		final boolean appendVersion = args.getBoolean(ARG_VERSION);
		if (appendVersion) {
			try {
				final String version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
				final String libVersion = BuildConfig.VERSION_NAME;
				text.append(getString(R.string.about_version, version));
				text.append(getString(R.string.about_dfu_version, libVersion));
			} catch (final NameNotFoundException e) {
				// do nothing
			}
		}
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.about_title).setMessage(text)
				.setPositiveButton(R.string.ok, null).create();
	}
}
