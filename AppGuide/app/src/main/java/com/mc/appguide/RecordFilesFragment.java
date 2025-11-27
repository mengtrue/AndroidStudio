package com.mc.appguide;

import android.app.Fragment;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordFilesFragment extends Fragment {
    private static final String TAG = "RecordFilesFragment";

    private LayoutInflater mLayoutInflater;
    private ListView mRecordListView;
    private RecordListAdapter mAdapter;
    private List<RecordFile> mRecordFiles = new ArrayList<>();
    private boolean mIsSelectionMode = false;
    private int mCurrentPlayingPosition = -1;
    private ActionMode mActionMode;
    private MediaPlayer mMediaPlayer;

    private AbsListView.MultiChoiceModeListener mMultiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            handleMultiChoiceModeItemChecked(position, checked);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return handleMultiChoiceModeActionClicked(item);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return handleMultiChoiceModeActionModeCreated(mode, menu);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            handleMultiChoiceModeActionDestroyed();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLayoutInflater = inflater;
        View view = inflater.inflate(R.layout.fragment_record_files, container, false);
        mRecordListView = view.findViewById(R.id.record_list);

        mMediaPlayer = new MediaPlayer();
        loadRecordFiles();

        mAdapter = new RecordListAdapter();
        mRecordListView.setAdapter(mAdapter);
        setupRecordListViewListeners();
        return view;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mRecordFiles.clear();
    }

    private void setupRecordListViewListeners() {
        mRecordListView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "OnItemClick selection mode : " + mIsSelectionMode);
            if (position < 0 || position >= mRecordFiles.size()) return;
            RecordFile recordFile = mRecordFiles.get(position);
            if (mIsSelectionMode) {
                recordFile.setSelected(!recordFile.isSelected());
                mAdapter.notifyDataSetChanged();
                updateActionModeTitle();
                mRecordListView.setItemChecked(position, recordFile.isSelected());
            } else {
                if (recordFile.isPlaying()) {
                    pauseRecordFile();
                } else {
                    if (mCurrentPlayingPosition != -1 && mCurrentPlayingPosition < mRecordFiles.size()) {
                        mRecordFiles.get(mCurrentPlayingPosition).setPlaying(false);
                    }
                    playRecordFile(position, recordFile.getFilePath());
                }
                mAdapter.notifyDataSetChanged();
            }
        });
        mRecordListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mRecordListView.setMultiChoiceModeListener(mMultiChoiceModeListener);
    }

    private void loadRecordFiles() {
        mRecordFiles.add(new RecordFile("春天的故事.mp3", 225000, "/sdcard/Music/spring_story.mp3"));
        mRecordFiles.add(new RecordFile("夏日微风.mp3", 184000, "/sdcard/Music/summer_breeze.mp3"));
        mRecordFiles.add(new RecordFile("秋日私语.mp3", 256000, "/sdcard/Music/autumn_whisper.mp3"));
        mRecordFiles.add(new RecordFile("冬季恋歌.mp3", 198000, "/sdcard/Music/winter_love.mp3"));
        mRecordFiles.add(new RecordFile("月光奏鸣曲.mp3", 312000, "/sdcard/Music/moonlight_sonata.mp3"));
        mRecordFiles.add(new RecordFile("雨中漫步.mp3", 176000, "/sdcard/Music/walking_in_rain.mp3"));
        mRecordFiles.add(new RecordFile("海边日出.mp3", 234000, "/sdcard/Music/sunrise_beach.mp3"));
        mRecordFiles.add(new RecordFile("山谷回声.mp3", 287000, "/sdcard/Music/valley_echo.mp3"));
    }

    private void updateActionModeTitle() {
        if (mActionMode != null) {
            int count = getSelectedCount();
            mActionMode.setTitle(count + " 已选择");
        }
    }

    private void handleMultiChoiceModeItemChecked(int position, boolean checked) {
        if (position >= 0 && position < mRecordFiles.size()) {
            mRecordFiles.get(position).setSelected(checked);
            mAdapter.notifyDataSetChanged();
            updateActionModeTitle();
        }
    }

    private boolean handleMultiChoiceModeActionModeCreated(ActionMode mode, Menu menu) {
        Log.d(TAG, "handleMultiChoiceModeActionModeCreated");
        try {
            mode.getMenuInflater().inflate(R.menu.menu_record_selection, menu);
            mIsSelectionMode = true;
            mActionMode = mode;
            updateActionModeTitle();
            mAdapter.notifyDataSetChanged();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean handleMultiChoiceModeActionClicked(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteSelectedFiles();
            return true;
        } else if (item.getItemId() == R.id.action_select_all) {
            updateSelectAllAction();
            return true;
        }
        return false;
    }

    private void handleMultiChoiceModeActionDestroyed() {
        Log.d(TAG, "handleMultiChoiceModeActionDestroyed, selection : " + mIsSelectionMode + ", actionMode : " + mActionMode);
        if (!mIsSelectionMode || mActionMode == null) return;
        mIsSelectionMode = false;
        mActionMode = null;
        clearSelection();
        mRecordListView.invalidate();
        mAdapter.notifyDataSetChanged();
    }

    private int getSelectedCount() {
        int count = 0;
        for (RecordFile file : mRecordFiles) {
            if (file.isSelected()) count++;
        }
        return count;
    }

    private void updateSelectAllAction() {
        boolean allSelected = true;
        for (RecordFile file : mRecordFiles) {
            if (!file.isSelected()) {
                allSelected = false;
                break;
            }
        }
        for (RecordFile file : mRecordFiles) {
            file.setSelected(!allSelected);
        }
        mAdapter.notifyDataSetChanged();
        updateActionModeTitle();
    }

    private void clearSelection() {
        for (RecordFile file : mRecordFiles) {
            file.setSelected(false);
        }
    }

    private void deleteSelectedFiles() {
        Log.d(TAG, "deleteSelectedFiles");
        List<RecordFile> removedFiles = new ArrayList<>();
        for (RecordFile file : mRecordFiles) {
            if (file.isSelected()) removedFiles.add(file);
        }
        if (mCurrentPlayingPosition != -1 && mRecordFiles.get(mCurrentPlayingPosition).isSelected()) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) mMediaPlayer.stop();
            mCurrentPlayingPosition = -1;
        }
        mRecordFiles.removeAll(removedFiles);
        resetRecordListView();
        mAdapter.notifyDataSetChanged();
        if (mActionMode != null) mActionMode.finish();
    }

    private void resetRecordListView() {
        clearSelection();
        if (mRecordFiles.isEmpty()) mRecordListView.setSelection(0);
    }

    private void playRecordFile(int position, String filePath) {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(filePath);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mRecordFiles.get(position).setPlaying(true);
            mCurrentPlayingPosition = position;
            mAdapter.notifyDataSetChanged();
            mMediaPlayer.setOnCompletionListener(mp -> {
                if (mCurrentPlayingPosition != -1) {
                    mRecordFiles.get(mCurrentPlayingPosition).setPlaying(false);
                    mCurrentPlayingPosition = -1;
                    mAdapter.notifyDataSetChanged();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseRecordFile() {
        if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
        if (mCurrentPlayingPosition != -1) {
            mRecordFiles.get(mCurrentPlayingPosition).setPlaying(false);
            mAdapter.notifyDataSetChanged();
        }
    }

    private static class RecordFile implements java.io.Serializable {
        private String title, filePath;
        private long durationMs;
        private boolean isPlaying, isSelected;

        public RecordFile(String title, long durationMs, String filePath) {
            this.title = title;
            this.durationMs = durationMs;
            this.filePath = filePath;
            this.isPlaying = false;
            this.isSelected = false;
        }

        public String getTitle() { return title; }
        public long getDuration() { return durationMs; }
        public String getFilePath() { return filePath; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { this.isSelected = selected; }
        public boolean isPlaying() { return isPlaying; }
        public void setPlaying(boolean playing) { this.isPlaying = playing; }
    }

    private class RecordListAdapter extends BaseAdapter {

        //How many items are in the data set represented by this Adapter
        @Override
        public int getCount() {
            return mRecordFiles.size();
        }

        //Get the data item associated with the specified position in the data set
        @Override
        public Object getItem(int position) {
            return mRecordFiles.get(position);
        }

        //Get the row id associated with the specified position in the list
        @Override
        public long getItemId(int position) {
            return position;
        }

        //Get a View that displays the data at the specified position in the data set
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d(TAG, "getview position : " + position + ", selectionmode : " + mIsSelectionMode);
            RecordFileViewHolder holder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.record_list_item, parent, false);
                holder = new RecordFileViewHolder();
                holder.tv_title = convertView.findViewById(R.id.record_title);
                holder.tv_duration = convertView.findViewById(R.id.record_duration);
                holder.cb_delete = convertView.findViewById(R.id.cb_delete);
                convertView.setTag(holder);
            } else {
                holder = (RecordFileViewHolder) convertView.getTag();
            }
            RecordFile recordFile = mRecordFiles.get(position);
            holder.tv_title.setText(recordFile.getTitle());
            holder.tv_duration.setText(formatDuration(recordFile.getDuration()));

            if (mIsSelectionMode) {
                holder.cb_delete.setVisibility(View.VISIBLE);
                holder.cb_delete.setChecked(recordFile.isSelected());
                holder.cb_delete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    recordFile.setSelected(isChecked);
                    mAdapter.notifyDataSetChanged();
                    updateActionModeTitle();
                });
            } else {
                holder.cb_delete.setVisibility(View.GONE);
            }
            return convertView;
        }

        private String formatDuration(long durationMs) {
            long seconds = durationMs / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private class RecordFileViewHolder {
        TextView tv_title;
        TextView tv_duration;
        CheckBox cb_delete;
    }
}
