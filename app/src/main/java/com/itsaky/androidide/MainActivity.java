/*
 * This file is part of AndroidIDE.
 *
 *
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.itsaky.androidide;

import static com.itsaky.androidide.R.id;
import static com.itsaky.androidide.R.string;
import static com.itsaky.androidide.models.prefs.GeneralPreferencesKt.NO_OPENED_PROJECT;
import static com.itsaky.androidide.models.prefs.GeneralPreferencesKt.getAutoOpenProjects;
import static com.itsaky.androidide.models.prefs.GeneralPreferencesKt.getConfirmProjectOpen;
import static com.itsaky.androidide.models.prefs.GeneralPreferencesKt.getLastOpenedProject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.text.HtmlCompat;

import com.blankj.utilcode.util.SizeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.itsaky.androidide.app.IDEActivity;
import com.itsaky.androidide.app.IDEApplication;
import com.itsaky.androidide.databinding.ActivityMainBinding;
import com.itsaky.androidide.fragments.MainFragment;
import com.itsaky.androidide.projects.ProjectManager;
import com.itsaky.androidide.utils.DialogUtils;
import com.itsaky.androidide.utils.Environment;
import com.itsaky.toaster.Toaster;
import com.itsaky.toaster.ToasterKt;

import java.io.File;

public class MainActivity extends IDEActivity {
  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!IDEApplication.isAbiSupported()) {
      showDeviceNotSupported();
      return;
    }

    if (!checkToolsIsInstalled()) {
      showDialogInstallJdkSdk();
    } else {
      openLastProject();
    }

    getSupportFragmentManager()
        .beginTransaction()
        .replace(id.container, new MainFragment(), MainFragment.TAG)
        .commit();
  }

  @Override
  protected void onStorageGranted() {}

  @Override
  protected void onStorageDenied() {
    ToasterKt.toast(string.msg_storage_denied, Toaster.Type.ERROR);
    finishAffinity();
  }

  @Override
  protected void preSetContentLayout() {
    SplashScreen.installSplashScreen(this);
  }

  @Override
  protected View bindLayout() {
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    return binding.getRoot();
  }

  private void showDialogInstallJdkSdk() {
    final var dp24 = SizeUtils.dp2px(24);
    final MaterialAlertDialogBuilder builder = DialogUtils.newMaterialDialogBuilder(this);
    builder.setTitle(string.title_warning);
    final var view = new TextView(this);
    view.setPaddingRelative(dp24, dp24, dp24, dp24);
    view.setText(
        HtmlCompat.fromHtml(
            getString(string.msg_require_install_jdk_and_android_sdk),
            HtmlCompat.FROM_HTML_MODE_COMPACT));
    view.setMovementMethod(LinkMovementMethod.getInstance());
    builder.setView(view);
    builder.setCancelable(false);
    builder.setPositiveButton(android.R.string.ok, (d, w) -> openTerminal());
    builder.setNegativeButton(android.R.string.cancel, (d, w) -> finishAffinity());
    builder.show();
  }

  private void openTerminal() {
    startActivity(new Intent(this, TerminalActivity.class));
  }

  private void showDeviceNotSupported() {
    final MaterialAlertDialogBuilder builder = DialogUtils.newMaterialDialogBuilder(this);
    builder.setTitle(string.title_device_not_supported);
    builder.setMessage(string.msg_device_not_supported);
    builder.setCancelable(false);
    builder.setPositiveButton(android.R.string.ok, (p1, p2) -> finishAffinity());
    builder.create().show();
  }

  private void openLastProject() {
    binding.getRoot().post(this::tryOpenLastProject);
  }

  private void tryOpenLastProject() {
    if (!getAutoOpenProjects()) {
      return;
    }

    final var openedProject = getLastOpenedProject();
    if (NO_OPENED_PROJECT.equals(openedProject)) {
      return;
    }

    if (TextUtils.isEmpty(openedProject)) {
      getApp();
      ToasterKt.toast(string.msg_opened_project_does_not_exist, Toaster.Type.INFO);
      return;
    }

    final var project = new File(openedProject);
    if (!project.exists()) {
      ToasterKt.toast(string.msg_opened_project_does_not_exist, Toaster.Type.INFO);
      return;
    }

    if (getConfirmProjectOpen()) {
      askProjectOpenPermission(project);
      return;
    }

    openProject(project);
  }

  private void askProjectOpenPermission(File root) {
    final MaterialAlertDialogBuilder builder = DialogUtils.newMaterialDialogBuilder(this);
    builder.setTitle(string.title_confirm_open_project);
    builder.setMessage(getString(string.msg_confirm_open_project, root.getAbsolutePath()));
    builder.setCancelable(false);
    builder.setPositiveButton(string.yes, (d, w) -> openProject(root));
    builder.setNegativeButton(string.no, null);
    builder.show();
  }

  public void openProject(@NonNull File root) {
    ProjectManager.INSTANCE.setProjectPath(root.getAbsolutePath());
    startActivity(new Intent(this, EditorActivity.class));
  }

  private boolean checkToolsIsInstalled() {
    return Environment.JAVA.exists() && Environment.ANDROID_HOME.exists();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }
}
