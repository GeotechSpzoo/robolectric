package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.M;
import static org.robolectric.shadow.api.Shadow.directlyOn;
import static org.robolectric.util.reflector.Reflector.reflector;

import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.SparseArray;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import com.google.common.base.Optional;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import org.robolectric.util.reflector.Accessor;
import org.robolectric.util.reflector.ForType;
import org.robolectric.util.reflector.Static;

@Implements(value = InputMethodManager.class)
public class ShadowInputMethodManager {

  /**
   * Handler for receiving soft input visibility changed event.
   *
   * Since Android does not have any API for retrieving soft input status, most application
   * relies on GUI layout changes to detect the soft input change event. Currently, Robolectric are
   * not able to simulate the GUI change when application changes the soft input through {@code
   * InputMethodManager}, this handler can be used by application to simulate GUI change in response
   * of the soft input change.
   */
  public interface SoftInputVisibilityChangeHandler {

    void handleSoftInputVisibilityChange(boolean softInputVisible);
  }

  private boolean softInputVisible;
  private Optional<SoftInputVisibilityChangeHandler> visibilityChangeHandler = Optional.absent();

  @Implementation
  protected boolean showSoftInput(View view, int flags) {
    return showSoftInput(view, flags, null);
  }

  @Implementation
  protected boolean showSoftInput(View view, int flags, ResultReceiver resultReceiver) {
    setSoftInputVisibility(true);
    return true;
  }

  @Implementation
  protected boolean hideSoftInputFromWindow(IBinder windowToken, int flags) {
    return hideSoftInputFromWindow(windowToken, flags, null);
  }

  @Implementation
  protected boolean hideSoftInputFromWindow(
      IBinder windowToken, int flags, ResultReceiver resultReceiver) {
    int resultCode;
    if (isSoftInputVisible()) {
      setSoftInputVisibility(false);
      resultCode = InputMethodManager.RESULT_HIDDEN;
    } else {
      resultCode = InputMethodManager.RESULT_UNCHANGED_HIDDEN;
    }

    if (resultReceiver != null) {
      resultReceiver.send(resultCode, null);
    }
    return true;
  }

  @Implementation
  protected void toggleSoftInput(int showFlags, int hideFlags) {
    setSoftInputVisibility(!isSoftInputVisible());
  }

  public boolean isSoftInputVisible() {
    return softInputVisible;
  }

  public void setSoftInputVisibilityHandler(
      SoftInputVisibilityChangeHandler visibilityChangeHandler) {
    this.visibilityChangeHandler =
        Optional.<SoftInputVisibilityChangeHandler>of(visibilityChangeHandler);
  }

  private void setSoftInputVisibility(boolean visible) {
    if (visible == softInputVisible) {
      return;
    }
    softInputVisible = visible;
    if (visibilityChangeHandler.isPresent()) {
      visibilityChangeHandler.get().handleSoftInputVisibilityChange(softInputVisible);
    }
  }

  @Implementation
  protected void restartInput(View view) {}

  @Implementation
  protected boolean isActive(View view) {
    return false;
  }

  @Implementation
  protected boolean isActive() {
    return false;
  }

  @Implementation
  protected boolean isFullscreenMode() {
    return false;
  }

  @Implementation
  protected void focusIn(View view) {}

  @Implementation(minSdk = M)
  protected void onViewDetachedFromWindow(View view) {}

  @Implementation
  protected void displayCompletions(View view, CompletionInfo[] completions) {}

  @Implementation(maxSdk = VERSION_CODES.LOLLIPOP_MR1)
  protected static InputMethodManager peekInstance() {
    // Android has a bug pre M where peekInstance was dereferenced without a null check:-
    // https://github.com/aosp-mirror/platform_frameworks_base/commit/a046faaf38ad818e6b5e981a39fd7394cf7cee03
    // So for earlier versions, just call through directly to getInstance()
    if (RuntimeEnvironment.getApiLevel() <= VERSION_CODES.JELLY_BEAN_MR1) {
      return ReflectionHelpers.callStaticMethod(
          InputMethodManager.class,
          "getInstance",
          ClassParameter.from(Looper.class, Looper.getMainLooper()));
    } else if (RuntimeEnvironment.getApiLevel() <= VERSION_CODES.LOLLIPOP_MR1) {
      return InputMethodManager.getInstance();
    }
    return directlyOn(InputMethodManager.class, "peekInstance");
  }

  @Implementation(minSdk = VERSION_CODES.N)
  protected boolean startInputInner(
      int startInputReason,
      IBinder windowGainingFocus,
      int startInputFlags,
      int softInputMode,
      int windowFlags) {
    return true;
  }

  @Resetter
  public static void reset() {
    int apiLevel = RuntimeEnvironment.getApiLevel();
    _InputMethodManager_ _reflector = reflector(_InputMethodManager_.class);
    if (apiLevel <= VERSION_CODES.JELLY_BEAN_MR1) {
      _reflector.setMInstance(null);
    } else if (apiLevel <= VERSION_CODES.P) {
      _reflector.setInstance(null);
    } else {
      _reflector.getInstanceMap().clear();
    }
  }

  @ForType(InputMethodManager.class)
  interface _InputMethodManager_ {
    @Static @Accessor("mInstance")
    void setMInstance(InputMethodManager instance);

    @Static @Accessor("sInstance")
    void setInstance(InputMethodManager instance);

    @Static @Accessor("sInstanceMap")
    SparseArray<InputMethodManager> getInstanceMap();
  }
}
