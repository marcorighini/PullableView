# PullableView
Enhanced Pull-To-Action layout

[![](https://jitpack.io/v/marcorighini/PullableView.svg)](https://jitpack.io/#marcorighini/PullableView)

## Dependency
Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Then, add the library to your module `build.gradle`
```gradle
dependencies {
    compile 'com.github.marcorighini:PullableView:latest.release.here'
}
```

## Features
- Pull gesture in UP, DOWN or BOTH directions
- Scroll thresholding. **Bound views** are snapped to **anchor** position if scroll over threshold else are **reset** to initial position
- Bind views for **animating** (alpha and translation for now) them in respect to scroll

## Usage
There is a [sample](https://github.com/marcorighini/PullableView/tree/master/app) provided which shows how to use the library. For completeness, here is all that is required to get PullableView working:

```xml
<com.cynny.cynny.misc.views.PullableView
    android:id="@+id/pullable_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    ....
</com.cynny.cynny.misc.views.PullableView>
```

## Attributes
Attributes that may be added to PullableView xml tag or changed programmatically through PullableView properties:
- anchorOffsetUp : integer corresponding to the anchor point for UP direction pull action. It's represented through the offset compared to the pullable_view top. It must be <= 0
- anchorOffsetDown : integer corresponding to the anchor point for DOWN direction pull action. It's represented through the offset compared to the pullable_view bottom. It must be >= 0
- scrollThresholdUp : integer corresponding to the threshold of the UP direction pull action. It must be <= 0. If scroll amount (negative) is below (greater in absolute value) this value bound views will animate to snap value (translation or alpha) else bound views will reset to initial values
- scrollThresholdDown : integer corresponding to the threshold of the DOWN direction pull action. It must be >= 0. If scroll amount is above this value bound views will animate to snap value (translation or alpha) else bound views will reset to initial values.
- direction : enum string value (UP,DOWN,BOTH) enabling pull directions

Default values for attributes are:
- anchorOffsetUp=-pullable_view_top 
- anchorOffsetDown=screen_height - pullable_view_bottom
- scrollThresholdUp=anchorOffsetUp/2
- scrollThresholdDown=anchorOffsetBottom/2
- direction=BOTH

## Bound Views and animations
PullableView has the property 
```kotlin
var boundViews = listOf<BoundView>()
```
This represents the views that are animated accordingly to the scroll amount. For example you can change the alpha or translation of these bound views. Note that tipically you have to add pullable_view to this list to have pull-to-refresh like gesture over it. 

Check the sample for usage.

## Snap and reset





