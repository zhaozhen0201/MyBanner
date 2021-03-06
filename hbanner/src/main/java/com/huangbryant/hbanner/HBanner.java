package com.huangbryant.hbanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.huangbryant.hbanner.listener.OnHBannerClickListener;
import com.huangbryant.hbanner.loader.ImageLoaderInterface;
import com.huangbryant.hbanner.view.HBannerViewPager;
import com.huangbryant.hbanner.view.TagImageView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.view.ViewPager.OnPageChangeListener;
import static android.support.v4.view.ViewPager.PageTransformer;

public class HBanner extends FrameLayout implements OnPageChangeListener {
    public String tag = "banner";
    private int mIndicatorMargin = HBannerConfig.PADDING_SIZE;
    private int mIndicatorWidth;
    private int mIndicatorHeight;
    private int indicatorSize;
    private int bannerBackgroundImage;
    private int bannerStyle = HBannerConfig.CIRCLE_INDICATOR;
    private int delayTime = HBannerConfig.TIME;
    private int scrollTime = HBannerConfig.DURATION;
    private boolean isAutoPlay = HBannerConfig.IS_AUTO_PLAY;
    private boolean isScroll = HBannerConfig.IS_SCROLL;
    private int mIndicatorSelectedResId = R.drawable.gray_radius;
    private int mIndicatorUnselectedResId = R.drawable.white_radius;
    private int mLayoutResId = R.layout.hbanner;
    private int titleHeight;
    private int titleBackground;
    private int titleTextColor;
    private int titleTextSize;
    private int count = 0;
    private int currentItem;
    private int gravity = -1;
    private int lastPosition = 1;
    private int scaleType = 1;
    private List<String> titles;
    private List imageUrls;
    private List<Boolean> imagesTag;
    private int resTag;
    private List<View> imageViews;
    private List<ImageView> indicatorImages;
    private Context context;
    private HBannerViewPager viewPager;
    private TextView bannerTitle, numIndicatorInside, numIndicator;
    private LinearLayout indicator, indicatorInside, titleView;
    private ImageView bannerDefaultImage;
    private ImageLoaderInterface imageLoader;
    private BannerPagerAdapter adapter;
    private OnPageChangeListener mOnPageChangeListener;
    private HBannerScroller mScroller;
    private OnHBannerClickListener listener;
    private DisplayMetrics dm;

    private WeakHandler handler = new WeakHandler();

    public HBanner(Context context) {
        this(context, null);
    }

    public HBanner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HBanner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        titles = new ArrayList<>();
        imageUrls = new ArrayList<>();
        imageViews = new ArrayList<>();
        indicatorImages = new ArrayList<>();
        dm = context.getResources().getDisplayMetrics();
        indicatorSize = dm.widthPixels / 80;
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        imageViews.clear();
        handleTypedArray(context, attrs);
        View view = LayoutInflater.from(context).inflate(mLayoutResId, this, true);
        bannerDefaultImage = (ImageView) view.findViewById(R.id.bannerDefaultImage);
        viewPager = (HBannerViewPager) view.findViewById(R.id.bannerViewPager);
        titleView = (LinearLayout) view.findViewById(R.id.titleView);
        indicator = (LinearLayout) view.findViewById(R.id.circleIndicator);
        indicatorInside = (LinearLayout) view.findViewById(R.id.indicatorInside);
        bannerTitle = (TextView) view.findViewById(R.id.bannerTitle);
        numIndicator = (TextView) view.findViewById(R.id.numIndicator);
        numIndicatorInside = (TextView) view.findViewById(R.id.numIndicatorInside);
        bannerDefaultImage.setImageResource(bannerBackgroundImage);
        initViewPagerScroll();
    }

    private void handleTypedArray(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HBanner);
        mIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.HBanner_indicator_width, indicatorSize);
        mIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.HBanner_indicator_height, indicatorSize);
        mIndicatorMargin = typedArray.getDimensionPixelSize(R.styleable.HBanner_indicator_margin, HBannerConfig.PADDING_SIZE);
        mIndicatorSelectedResId = typedArray.getResourceId(R.styleable.HBanner_indicator_drawable_selected, R.drawable.gray_radius);
        mIndicatorUnselectedResId = typedArray.getResourceId(R.styleable.HBanner_indicator_drawable_unselected, R.drawable.white_radius);
        scaleType = typedArray.getInt(R.styleable.HBanner_image_scale_type, scaleType);
        delayTime = typedArray.getInt(R.styleable.HBanner_delay_time, HBannerConfig.TIME);
        scrollTime = typedArray.getInt(R.styleable.HBanner_scroll_time, HBannerConfig.DURATION);
        isAutoPlay = typedArray.getBoolean(R.styleable.HBanner_is_auto_play, HBannerConfig.IS_AUTO_PLAY);
        titleBackground = typedArray.getColor(R.styleable.HBanner_title_background, HBannerConfig.TITLE_BACKGROUND);
        titleHeight = typedArray.getDimensionPixelSize(R.styleable.HBanner_title_height, HBannerConfig.TITLE_HEIGHT);
        titleTextColor = typedArray.getColor(R.styleable.HBanner_title_textcolor, HBannerConfig.TITLE_TEXT_COLOR);
        titleTextSize = typedArray.getDimensionPixelSize(R.styleable.HBanner_title_textsize, HBannerConfig.TITLE_TEXT_SIZE);
        mLayoutResId = typedArray.getResourceId(R.styleable.HBanner_banner_layout, mLayoutResId);
        bannerBackgroundImage = typedArray.getResourceId(R.styleable.HBanner_banner_default_image, R.drawable.no_banner);
        typedArray.recycle();
    }

    private void initViewPagerScroll() {
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            mScroller = new HBannerScroller(viewPager.getContext());
            mScroller.setDuration(scrollTime);
            mField.set(viewPager, mScroller);
        } catch (Exception e) {
            Log.e(tag, e.getMessage());
        }
    }


    public HBanner isAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay = isAutoPlay;
        return this;
    }

    public HBanner setImageLoader(ImageLoaderInterface imageLoader) {
        this.imageLoader = imageLoader;
        return this;
    }

    public HBanner setDelayTime(int delayTime) {
        this.delayTime = delayTime;
        return this;
    }

    public HBanner setIndicatorGravity(int type) {
        switch (type) {
            case HBannerConfig.LEFT:
                this.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                break;
            case HBannerConfig.CENTER:
                this.gravity = Gravity.CENTER;
                break;
            case HBannerConfig.RIGHT:
                this.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                break;
        }
        return this;
    }

    public HBanner setBannerAnimation(Class<? extends PageTransformer> transformer) {
        try {
            setPageTransformer(true, transformer.newInstance());
        } catch (Exception e) {
            Log.e(tag, "Please set the PageTransformer class");
        }
        return this;
    }

    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     * @return Banner
     */
    public HBanner setOffscreenPageLimit(int limit) {
        if (viewPager != null) {
            viewPager.setOffscreenPageLimit(limit);
        }
        return this;
    }

    /**
     * Set a {@link PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding look and feel.
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     * @return Banner
     */
    public HBanner setPageTransformer(boolean reverseDrawingOrder, PageTransformer transformer) {
        viewPager.setPageTransformer(reverseDrawingOrder, transformer);
        return this;
    }

    public HBanner setBannerTitles(List<String> titles) {
        this.titles = titles;
        return this;
    }

    public HBanner setBannerStyle(int bannerStyle) {
        this.bannerStyle = bannerStyle;
        return this;
    }

    public HBanner setViewPagerIsScroll(boolean isScroll) {
        this.isScroll = isScroll;
        return this;
    }

    public HBanner setImages(List<?> imageUrls) {
        this.imageUrls = imageUrls;
        this.count = imageUrls.size();
        return this;
    }

    private HBanner setImagesTag(List<Boolean> imagesTag, int resTag) {
        this.resTag = resTag;
        this.imagesTag = imagesTag;
        return this;
    }

    public void update(List<?> imageUrls, List<String> titles) {
        this.titles.clear();
        this.titles.addAll(titles);
        update(imageUrls);
    }

    public void update(List<?> imageUrls) {
        this.imageUrls.clear();
        this.imageViews.clear();
        this.indicatorImages.clear();
        this.imageUrls.addAll(imageUrls);
        this.count = this.imageUrls.size();
        start();
    }

    public void updateBannerStyle(int bannerStyle) {
        indicator.setVisibility(GONE);
        numIndicator.setVisibility(GONE);
        numIndicatorInside.setVisibility(GONE);
        indicatorInside.setVisibility(GONE);
        bannerTitle.setVisibility(View.GONE);
        titleView.setVisibility(View.GONE);
        this.bannerStyle = bannerStyle;
        start();
    }

    public HBanner start() {
        setBannerStyleUI();
        setImageList(imageUrls);
        setData();
        return this;

    }


    private void setTitleStyleUI() {
        if (titles.size() != imageUrls.size()) {
            throw new RuntimeException("[Banner] --> The number of titles and images is different");
        }
        if (titleBackground != -1) {
            titleView.setBackgroundColor(titleBackground);
        }
        if (titleHeight != -1) {
            titleView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, titleHeight));
        }
        if (titleTextColor != -1) {
            bannerTitle.setTextColor(titleTextColor);
        }
        if (titleTextSize != -1) {
            bannerTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize);
        }
        if (titles != null && titles.size() > 0) {
            bannerTitle.setText(titles.get(0));
            bannerTitle.setVisibility(View.VISIBLE);
            titleView.setVisibility(View.VISIBLE);
        }
    }

    private void setBannerStyleUI() {
        int visibility = count > 1 ? View.VISIBLE : View.GONE;
        switch (bannerStyle) {
            case HBannerConfig.CIRCLE_INDICATOR:
                indicator.setVisibility(visibility);
                break;
            case HBannerConfig.NUM_INDICATOR:
                numIndicator.setVisibility(visibility);
                break;
            case HBannerConfig.NUM_INDICATOR_TITLE:
                numIndicatorInside.setVisibility(visibility);
                setTitleStyleUI();
                break;
            case HBannerConfig.CIRCLE_INDICATOR_TITLE:
                indicator.setVisibility(visibility);
                setTitleStyleUI();
                break;
            case HBannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE:
                indicatorInside.setVisibility(visibility);
                setTitleStyleUI();
                break;
        }
    }

    private void initImages() {
        imageViews.clear();
        if (bannerStyle == HBannerConfig.CIRCLE_INDICATOR ||
                bannerStyle == HBannerConfig.CIRCLE_INDICATOR_TITLE ||
                bannerStyle == HBannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE) {
            createIndicator();
        } else if (bannerStyle == HBannerConfig.NUM_INDICATOR_TITLE) {
            numIndicatorInside.setText("1/" + count);
        } else if (bannerStyle == HBannerConfig.NUM_INDICATOR) {
            numIndicator.setText("1/" + count);
        }
    }

    private void setImageList(List<?> imagesUrl) {
        if (imagesUrl == null || imagesUrl.size() <= 0) {
            bannerDefaultImage.setVisibility(VISIBLE);
            Log.e(tag, "The image data set is empty.");
            return;
        }
        bannerDefaultImage.setVisibility(GONE);
        initImages();
        for (int i = 0; i <= count + 1; i++) {
            int position = 0;
            View imageView = null;
            TagImageView tagImageView = null;
            if (imageLoader != null && imageLoader.createImageView(context) != null) {
                imageView = imageLoader.createImageView(context);
            }
//            if (imageLoader != null && imageLoader.createTagImageView(context) != null) {
//                try {
//                    tagImageView = imageLoader.createTagImageView(context);
//                    if (i == 0) {
//                        tagImageView.setCenterImgShow(imagesTag.get(count - 1), resTag);
//                    } else if (i == count + 1) {
//                        tagImageView.setCenterImgShow(imagesTag.get(0), resTag);
//                    } else {
//                        tagImageView.setCenterImgShow(imagesTag.get(i - 1), resTag);
//                    }
//                    imageView = tagImageView;
//                } catch (Exception e) {
//                    imageView = imageLoader.createImageView(context);
//                }
//            }
            if (imageView == null) {
                imageView = new ImageView(context);
            }
            setScaleType(imageView);
            Object url = null;
            if (i == 0) {
                position = count - 1;
                url = imagesUrl.get(count - 1);
            } else if (i == count + 1) {
                position = 0;
                url = imagesUrl.get(0);
            } else {
                position = i - 1;
                url = imagesUrl.get(i - 1);
            }
            imageViews.add(imageView);
            if (imageLoader != null)
                imageLoader.displayImage(context, url, imageView, position);
            else
                Log.e(tag, "Please set images loader.");
        }
    }

    private void setScaleType(View imageView) {
        if (imageView instanceof ImageView) {
            ImageView view = ((ImageView) imageView);
            switch (scaleType) {
                case 0:
                    view.setScaleType(ScaleType.CENTER);
                    break;
                case 1:
                    view.setScaleType(ScaleType.CENTER_CROP);
                    break;
                case 2:
                    view.setScaleType(ScaleType.CENTER_INSIDE);
                    break;
                case 3:
                    view.setScaleType(ScaleType.FIT_CENTER);
                    break;
                case 4:
                    view.setScaleType(ScaleType.FIT_END);
                    break;
                case 5:
                    view.setScaleType(ScaleType.FIT_START);
                    break;
                case 6:
                    view.setScaleType(ScaleType.FIT_XY);
                    break;
                case 7:
                    view.setScaleType(ScaleType.MATRIX);
                    break;
            }

        }
    }

    private void createIndicator() {
        indicatorImages.clear();
        indicator.removeAllViews();
        indicatorInside.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mIndicatorWidth, mIndicatorHeight);
            params.leftMargin = mIndicatorMargin;
            params.rightMargin = mIndicatorMargin;
            if (i == 0) {
                imageView.setImageResource(mIndicatorSelectedResId);
            } else {
                imageView.setImageResource(mIndicatorUnselectedResId);
            }
            indicatorImages.add(imageView);
            if (bannerStyle == HBannerConfig.CIRCLE_INDICATOR ||
                    bannerStyle == HBannerConfig.CIRCLE_INDICATOR_TITLE)
                indicator.addView(imageView, params);
            else if (bannerStyle == HBannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE)
                indicatorInside.addView(imageView, params);
        }
    }


    private void setData() {
        currentItem = 1;
        if (adapter == null) {
            adapter = new BannerPagerAdapter();
            viewPager.addOnPageChangeListener(this);
        }
        viewPager.setAdapter(adapter);
        viewPager.setFocusable(true);
        viewPager.setCurrentItem(1);
        if (gravity != -1)
            indicator.setGravity(gravity);
        if (isScroll && count > 1) {
            viewPager.setScrollable(true);
        } else {
            viewPager.setScrollable(false);
        }
        if (isAutoPlay)
            startAutoPlay();
    }


    public void startAutoPlay() {
        handler.removeCallbacks(task);
        handler.postDelayed(task, delayTime);
    }

    public void stopAutoPlay() {
        handler.removeCallbacks(task);
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if (count > 1 && isAutoPlay) {
                currentItem = currentItem % (count + 1) + 1;
//                Log.i(tag, "curr:" + currentItem + " count:" + count);
                if (currentItem == 1) {
                    viewPager.setCurrentItem(currentItem, false);
                    handler.post(task);
                } else {
                    viewPager.setCurrentItem(currentItem);
                    handler.postDelayed(task, delayTime);
                }
            }
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        Log.i(tag, ev.getAction() + "--" + isAutoPlay);
        if (isAutoPlay) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_OUTSIDE) {
                startAutoPlay();
            } else if (action == MotionEvent.ACTION_DOWN) {
                stopAutoPlay();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 返回真实的位置
     *
     * @param position
     * @return 下标从0开始
     */
    public int toRealPosition(int position) {
        int realPosition = (position - 1) % count;
        if (realPosition < 0)
            realPosition += count;
        return realPosition;
    }

    class BannerPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imageViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            container.addView(imageViews.get(position));
            View view = imageViews.get(position);
            if (listener != null) {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.OnBannerClick(toRealPosition(position));
                    }
                });
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(state);
        }
//        Log.i(tag,"currentItem: "+currentItem);
        switch (state) {
            case 0://No operation
                if (currentItem == 0) {
                    viewPager.setCurrentItem(count, false);
                } else if (currentItem == count + 1) {
                    viewPager.setCurrentItem(1, false);
                }
                break;
            case 1://start Sliding
                if (currentItem == count + 1) {
                    viewPager.setCurrentItem(1, false);
                } else if (currentItem == 0) {
                    viewPager.setCurrentItem(count, false);
                }
                break;
            case 2://end Sliding
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(toRealPosition(position), positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        currentItem = position;
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageSelected(position);
        }

        if (bannerStyle == HBannerConfig.CIRCLE_INDICATOR ||
                bannerStyle == HBannerConfig.CIRCLE_INDICATOR_TITLE ||
                bannerStyle == HBannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE) {
            indicatorImages.get((lastPosition - 1 + count) % count).setImageResource(mIndicatorUnselectedResId);
            indicatorImages.get((position - 1 + count) % count).setImageResource(mIndicatorSelectedResId);
            lastPosition = position;
        }
        if (position == 0) position = count;
        if (position > count) position = 1;
        switch (bannerStyle) {
            case HBannerConfig.CIRCLE_INDICATOR:
                break;
            case HBannerConfig.NUM_INDICATOR:
                numIndicator.setText(position + "/" + count);
                break;
            case HBannerConfig.NUM_INDICATOR_TITLE:
                numIndicatorInside.setText(position + "/" + count);
                bannerTitle.setText(titles.get(position - 1));
                break;
            case HBannerConfig.CIRCLE_INDICATOR_TITLE:
                bannerTitle.setText(titles.get(position - 1));
                break;
            case HBannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE:
                bannerTitle.setText(titles.get(position - 1));
                break;
        }

    }


    /**
     * @param listener
     * @return
     */
    public HBanner setOnBannerListener(OnHBannerClickListener listener) {
        this.listener = listener;
        return this;
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        mOnPageChangeListener = onPageChangeListener;
    }


    public void releaseBanner() {
        handler.removeCallbacksAndMessages(null);
    }
}
