package com.example.android.emojify;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by ctyeung on 1/6/18.
 */

public class EmojiFragment extends Fragment
{
    private ImageView imageView;
    private View rootView;
    private int mListIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle saveInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_emoji, container, false);
        imageView = (ImageView) rootView.findViewById(R.id.iv_emoji);
        imageView.setImageResource(EmojiImageAssets.getHeads().get(mListIndex));
        return rootView;
    }

    public void setIndex(int index)
    {
        mListIndex = index;
    }
}
