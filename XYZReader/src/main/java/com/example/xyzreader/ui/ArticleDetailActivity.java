package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you
 * swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    //private View mUpButton;
    private Toolbar mUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set full screen mode for Lillipop and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);
        getLoaderManager().initLoader(0, null, this);

        // Initialize the pager and bind the data with the veiws
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        // TODO: Look into whether this should be moved to a style
        mPager.setPageMargin((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1,
                getResources().getDisplayMetrics()));

        // Setting a color drawable that will fill the margin between pages.
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        // Register the needed pager listeners
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            /**
             * Called when the scroll state changes. Useful for discovering when the
             * user begins dragging, when the pager is automatically settling to the
             * current page, or when it is fully stopped/idle.
             *
             * @param state The new scroll state.
             */
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }

            /**
             * This method will be invoked when a new page becomes selected.
             * Animation is not necessarily complete.
             *
             * @param position Position index of the new selected page.
             */
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                updateUpButtonPosition();
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = (Toolbar) findViewById(R.id.action_up);
        mUpButton.setTitle("");
        setSupportActionBar(mUpButton);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSupportNavigateUp();
            }
        });

        // Modify window behavor for Lollipop and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mUpButtonContainer.setOnApplyWindowInsetsListener(
//                    new View.OnApplyWindowInsetsListener() {
//                @Override
//                public WindowInsets onApplyWindowInsets(View view,
//                                                        WindowInsets windowInsets) {
//                    view.onApplyWindowInsets(windowInsets);
//                    mTopInset = windowInsets.getSystemWindowInsetTop();
//                    mUpButtonContainer.setTranslationY(mTopInset);
//                    updateUpButtonPosition();
//                    return windowInsets;
//                }
//            });
//        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id     The ID whose loader is to be created.
     * @param bundle Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    /**
     * Called when a previously created loader has finished its load.
     *
     * @param cursorLoader The Loader that has finished.
     * @param cursor       The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    /**
     * Called when a previously created loader is being reset,
     * and thus making its data unavailable.
     *
     * @param cursorLoader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        /**
         * Called to inform the adapter of which item is currently considered
         * to be the "primary", that is the one show to the user as the current page.
         *
         * @param container The containing View from which the page will be removed.
         * @param position  The page position that is now the primary.
         * @param object    The same object that was returned by instantiateItem(View, int).
         */
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            ArticleDetailFragment fragment = (ArticleDetailFragment) object;

            if (fragment != null) {
                mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        /**
         * Return the Fragment associated with a specified position.
         */
        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment
                    .newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        /**
         * Returns the numbers of rows in the cursor.
         */
        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
