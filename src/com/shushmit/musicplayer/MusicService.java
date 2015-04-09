package com.shushmit.musicplayer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;

public class MusicService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener {

	private String songTitle = "";
	private static final int NOTIFY_ID = 1;
	private boolean shuffle = false;
	private Random rand;
	// media player
	private MediaPlayer player;
	// song list
	private ArrayList<Song> songs;
	// current position
	private int songPosn;

	private final IBinder musicBind = new MusicBinder();

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		rand = new Random();
		songPosn = 0;
		player = new MediaPlayer();
		initMusicPlayer();
	}

	public void initMusicPlayer() {
		// set player properties
		player.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}

	public void setList(ArrayList<Song> theSongs) {
		songs = theSongs;
	}

	public class MusicBinder extends Binder {
		MusicService getService() {
			return MusicService.this;
		}
	}

	public void setShuffle() {
		if (shuffle)
			shuffle = false;
		else
			shuffle = true;
	}

	public void playNext() {
		if (shuffle) {
			int newSong = songPosn;
			while (newSong == songPosn) {
				newSong = rand.nextInt(songs.size());
			}
			songPosn = newSong;
		} else {
			songPosn++;
			if (songPosn >= songs.size())
				songPosn = 0;
		}
		playSong();
	}

	public void playPrev() {

		songPosn--;

		if (songPosn < 0)
			songPosn = songs.size() - 1;
		playSong();
	}

	public void playSong() {
		// play a song
		player.reset();
		// get song
		Song playSong = songs.get(songPosn);
		songTitle = playSong.getTitle();
		// get id
		long currSong = playSong.getID();
		// set uri
		Uri trackUri = ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				currSong);
		try {
			player.setDataSource(getApplicationContext(), trackUri);
		} catch (Exception e) {
			Log.e("MUSIC SERVICE", "Error setting data source", e);
		}
		player.prepareAsync();
	}

	public int getPosn() {
		return player.getCurrentPosition();
	}

	public int getDur() {
		return player.getDuration();
	}

	public boolean isPng() {
		return player.isPlaying();
	}

	public void pausePlayer() {
		player.pause();
	}

	public void seek(int posn) {
		player.seekTo(posn);
	}

	@SuppressLint("NewApi")
	public void go() {
		player.start();

	}

	@Override
	public void onDestroy() {
		stopForeground(true);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return musicBind;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		player.stop();
		player.release();
		return false;
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		return false;
	}

	public void onCompletion(MediaPlayer mp) {
		if (player.getCurrentPosition() > 0) {
			mp.reset();
			playNext();
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		mp.start();
		Intent notIntent = new Intent(this, MainActivity.class);
		notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification.Builder builder = new Notification.Builder(this);

		builder.setContentIntent(pendInt).setSmallIcon(R.drawable.play)
				.setTicker(songTitle).setOngoing(true)
				.setContentTitle("Playing").setContentText(songTitle);
		Notification not = builder.build();

		startForeground(NOTIFY_ID, not);
	}

	public void setSong(int songIndex) {
		songPosn = songIndex;
	}

}
