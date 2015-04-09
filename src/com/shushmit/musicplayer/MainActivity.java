package com.shushmit.musicplayer;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import com.shushmit.musicplayer.MusicService.MusicBinder;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public class MainActivity extends Activity implements MediaPlayerControl {

	private ArrayList<Song> songList;
	private ListView songView;
	private MusicController controller;
	private boolean paused = false, playbackPaused = false;
	private MusicService musicSrv;
	private Intent playIntent;
	private boolean musicBound = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		songView = (ListView) findViewById(R.id.songs_list);
		songList = new ArrayList<Song>();
		getSongList();

		Collections.sort(songList, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.getTitle().compareTo(b.getTitle());
			}
		});

		SongAdapter songAdt = new SongAdapter(this, songList);
		songView.setAdapter(songAdt);
		setController();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (playIntent == null) {
			playIntent = new Intent(this, MusicService.class);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	}

	// connect to the service
	private ServiceConnection musicConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MusicBinder binder = (MusicBinder) service;
			// get service
			musicSrv = binder.getService();
			// pass list
			musicSrv.setList(songList);
			musicBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			musicBound = false;
		}
	};

	private void setController() {
		// set the controller up
		controller = new MusicController(this);
		controller.setMediaPlayer(this);
		controller.setAnchorView(findViewById(R.id.songs_list));
		controller.setEnabled(true);
		controller.setPrevNextListeners(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playNext();
			}
		}, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playPrev();
			}
		});
	}

	private void playNext() {
		musicSrv.playNext();
		if (playbackPaused) {
			setController();
			playbackPaused = false;
		}
		controller.show(0);
	}

	private void playPrev() {
		musicSrv.playPrev();
		if (playbackPaused) {
			setController();
			playbackPaused = false;
		}
		controller.show(0);
	}

	public void songPicked(View view) {
		musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
		musicSrv.playSong();
		if (playbackPaused) {
			setController();
			playbackPaused = false;
		}
		controller.show(0);
	}

	public void getSongList() {
		// retrieve song info
		ContentResolver musicResolver = getContentResolver();
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor musicCursor = musicResolver.query(musicUri, null, null, null,
				null);
		if (musicCursor != null && musicCursor.moveToFirst()) {
			// get columns
			int titleColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
			int idColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
			int artistColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
			// add songs to list
			do {
				long thisId = musicCursor.getLong(idColumn);
				String thisTitle = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				songList.add(new Song(thisId, thisTitle, thisArtist));
			} while (musicCursor.moveToNext());
		}
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		if (musicSrv != null && musicBound && musicSrv.isPng())
			return musicSrv.getPosn();
		else
			return 0;
	}

	@Override
	public int getDuration() {
		if (musicSrv != null && musicBound && musicSrv.isPng())
			return musicSrv.getDur();
		else
			return 0;
	}

	@Override
	public boolean isPlaying() {
		if (musicSrv != null && musicBound)
			return musicSrv.isPng();
		return false;
	}

	@Override
	public void pause() {
		playbackPaused = true;
		musicSrv.pausePlayer();
	}

	@Override
	public void seekTo(int pos) {
		// TODO Auto-generated method stub
		musicSrv.seek(pos);

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		musicSrv.go();

	}

	@Override
	protected void onPause() {
		super.onPause();
		paused = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (paused) {
			setController();
			paused = false;
		}
	}

	@Override
	protected void onStop() {
		controller.hide();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		stopService(playIntent);
		musicSrv = null;
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.action_shuffle:
			// shuffle
			break;
		case R.id.action_end:
			stopService(playIntent);
			musicSrv = null;
			System.exit(0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
	    //  Action to be performed 
	    super.onBackPressed(); 
	}
}
