package com.example.pomodoro

import android.graphics.Color
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.Timer
import kotlin.concurrent.timer

/*
백그라운드 번쩍이게 하는 작업 ㄱㄱ
 */

class MainActivity : AppCompatActivity() {
    enum class State{
        CONCENT, BREAK, REST, OFF
    }
    private val COUNT: Int = 4              // 사이클 횟수
    private val REST_TIME: Int = 0         // 휴식시간
    private val BREAK_TIME: Int = 0         // 쉬는시간
    private val CONCENT_TIME: Int = 0      // 집중시간
    private val DELAY_TIME: Int = 5         // 딜레이 타임

    private var state: State = State.OFF    // 현재 상태

    // 타이머 관련 변수
    private var tt: Timer? = null           // 타이머
    private var isRunning: Boolean = false   // 타이머 동작중인지 판별
//    private var isBreak: Boolean = false    // 쉬는시간 여부
//    private var isRest: Boolean = false     // 휴식시간 여부
    private var mm: Int = 0                 // 분
    private var ss: Int = DELAY_TIME        // 초
    private var cnt: Int = 0                // 현재 사이클 몇 번 돌았는지 판단

    private val soundPool: SoundPool = SoundPool.Builder().build()

    override fun onResume(){
        super.onResume()
        soundPool.autoResume()
    }

    override fun onPause(){
        super.onPause()
        soundPool.autoPause()
    }

    override fun onDestroy(){
        super.onDestroy()
        soundPool.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_start.setOnClickListener{
            if(!isRunning) {
                btn_start.text = "STOP"
                timerOn()
            }
            else {
                btn_start.text = "START"
                state = State.OFF
                timerOff()
            }
        }
    }

    // 타이머 시작
    private fun timerOn(){
        // 동작 상태로 변경
        isRunning = true
        state = State.CONCENT

        // 타이머 시작 (쓰레드 작업)
        tt = timer(period = 1000){
            runOnUiThread{
                if(!isRunning) timerOff()    // 실행중이 아니면 멈춤
                printTime()
            }
        }
    }

    // 타이머 종료
    private fun timerOff(){
        isRunning = false
        updateCount()
        setTime()
        printTime()
        tt?.cancel()
    }

    // 타이머 동작
    private fun updateTime(){
        if(isRunning){ // TODO: 이 조건문이 필요한지 고민하기
            // 초 줄이기
            ss--
            // 0초 밑으로 내려가면 분 줄이고 59초로 만들기
            if(mm > 0 && ss < 0){
                ss += 60
                mm--
            }
            else if(mm == 0 && ss <= 0){
                // 상태에 따라
                when(state){
                    State.CONCENT ->
                    {
                        cnt++;
                        // 카운트에 따라 휴식/쉬는시간 정하기
                        if(cnt >= COUNT ){
                            state = State.REST
                        }else{
                            state = State.BREAK
                        }
                        updateCount()
                    }
                    State.REST, State.BREAK ->
                    {
                        // REST 였으면 cnt 다시 0으로 초기화 <- 휴식시간 끝났기 때문
                        if(state == State.REST) cnt = 0
                        // 휴식/쉬는시간이 끝나면 일시정지 되어야 하므로 OFF 상태로 만들기
                        state = State.OFF
                        timerOff()
                        btn_start.text = "START"
                    }
                    State.OFF -> Log.e("ERR", "타이머가 종료되어도 실행됨");
                }
                background()
                setTime()
            }
        }
    }

    private fun setTime(){
        when(state){
            State.OFF, State.CONCENT -> mm = CONCENT_TIME
            State.REST -> mm = REST_TIME
            State.BREAK -> mm = BREAK_TIME
        }
        ss = DELAY_TIME
    }

    private fun printTime(){
        updateTime()
        txt_timer.text = String.format("%02d",mm).plus(":").plus(String.format("%02d",ss))
    }

    /**
     * 상황에 맞는 txt_count 출력
     * 강제로 멈추지 않는다면 cnt++
     * 만약 cnt가 COUNT보다 커진다면 긴 휴식 시간
     */
    private fun updateCount(){
        when(state){
            State.OFF, State.CONCENT
                -> txt_count.text = cnt.toString().plus("/").plus(COUNT)
            State.BREAK -> txt_count.text = "Take A Break!!"
            State.REST -> {txt_count.text = "Get Some Rest!!"; cnt = 0}
        }
    }

    // TODO: 나중에 이름 바꾸기
    private fun background(){
        var b = false
        var cnt = 0
        val alertSoundId: Int = soundPool.load(this, R.raw.alert, 1);
        timer(period = 500){
            runOnUiThread{
                if(isRunning) {
                    if (!b) backLayout.setBackgroundColor(Color.RED)
                    else backLayout.setBackgroundColor(Color.BLACK)
                    b = !b
                    if(++cnt >= 10) this?.cancel()
                    alertSoundId?.let{alertSoundId->soundPool.play(alertSoundId,1f, 1f, 0, 0, 1f)}
                }else{
                    backLayout.setBackgroundColor(Color.BLACK)
                    this?.cancel()
                }
            }
        }
    }
}