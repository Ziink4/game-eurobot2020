<!-- LEAGUES level1 level2 -->
<div id="statement_back" class="statement_back" style="display: none"></div>
<div class="statement-body">
  <!-- GOAL -->
  <div class="statement-section statement-goal">
    <h2>
      <span class="icon icon-goal">&nbsp;</span> <span>The Goal</span>
    </h2>
    <div class="statement-goal-content">Code an IA for the eurobot 2020 contest</div>
    <!-- BEGIN level1 -->
    <br />Level 1: You are not yet <i>homologated</i>. You need to you need to be able to win a match with no opponent.
    <!-- END -->
  </div>

  <!-- RULES -->
  <div class="statement-section statement-rules">
      <h1>
          <span class="icon icon-rules">&nbsp;</span>
          <span>Rules</span>
      </h1>
      <div class="statement-rules-content">
          <p>Please read the eurobot 2020 <a href="https://www.eurobot.org/images/2020/Eurobot2020_Rules_Cup_OFFICIAL_EN.pdf">contest rules</a>.</p>
<br />
          <p>Informations :
	      	<ul>
		      	<li>The height of the robot is 150 mm</li>
		      	<li>The width of the robot is 250 mm</li>
		      	<li>Encoder values are in multiple of 0.1mm</li>
		      	<li>The distance between the two encoders is 250 mm</li>
		      	<li>Each robot can only hold up to 4 playing elements</li>
		      	<li>Each turn is 350 ms long</li>
	      	</ul>
	      </p>
      </div>
  </div>

  <!-- PROTOCOL -->
  <div class="statement-section statement-protocol">
    <h2>
      <span class="icon icon-protocol">&nbsp;</span>
      <span>Game Input</span>
    </h2>
    <!-- Protocol block -->
    <div class="blk">
      <div class="title">Input before the first turn</div>
      <div class="text">
		<p>
		    <span class="statement-lineno">Line 1:</span> <var>player_color</var><br>
		    <span class="statement-lineno">Line 2:</span> <var>x</var> <var>y</var> <var>a</var> for robot 1<br>
		    <span class="statement-lineno">Line 3:</span> <var>x</var> <var>y</var> <var>a</var> for robot 2<br>
		     <ul>
		      	<li><var>player_color</var> can be "BLUE" or "YELLOW"</li>
		      	<li><var>x</var> and <var>y</var> are in mm</li>
		      	<li><var>a</var> is in degree</li>
		  	</ul>
		</p>
	  <div>
      <div class="title">Input for one game turn</div>
      <div class="text">
		<p>
		    <span class="statement-lineno">Line 1:</span> <var>left_encoder</var> <var>right_encoder</var> <var>last_taken_color</var> <var>detected_compass</var> for robot 1<br>
		    <span class="statement-lineno">Line 2:</span> <var>left_encoder</var> <var>right_encoder</var> <var>last_taken_color</var> <var>detected_compass</var> for robot 2<br>
		    <span class="statement-lineno">Line 3:</span> <var>front_low_sensor</var> <var>right_low_sensor</var> <var>back_low_sensor</var> <var>left_low_sensor</var> <var>front_left_low_sensor</var> <var>front_right_low_sensor</var> <var>back_right_low_sensor</var> <var>back_left_low_sensor</var> for robot 1<br>
		    <span class="statement-lineno">Line 4:</span> <var>front_low_sensor</var> <var>right_low_sensor</var> <var>back_low_sensor</var> <var>left_low_sensor</var> <var>front_left_low_sensor</var> <var>front_right_low_sensor</var> <var>back_right_low_sensor</var> <var>back_left_low_sensor</var> for robot 2<br>  		    	
		    <span class="statement-lineno">Line 5:</span> <var>lidar_data</var> for robot 1<br>
		    <span class="statement-lineno">Line 6:</span> <var>lidar_data</var> for robot 2<br>
		    <ul>
		    	<li><var>detected_compass</var> can be "?", "N" or "S". Value is valid only when robot is oriented toward the compass</li>
		      	<li><var>left_encoder</var> and <var>right_encoder</var> are signed integers</li>
		      	<li><var>last_taken_color</var> can be "?", "GREEN" or "RED"</li>
		      	<li>sensor data are in mm</li>
		      	<li><var>lidar_data</var> is in array of 360 integers
		  	</ul>
		</p>
	  </div>
	 <div class="title">Informations</div>
      <div class="text">
		<p>
		  	
		  </p>
      </div>
    </div>

    <!-- Protocol block -->
    <div class="blk">
      <div class="title">Output for one game turn</div>
      <div class="text">
      	<p>
		    <span class="statement-lineno">Line 1:</span> <var>left_motor</var> <var>right_motor</var> <var>mechanical_order</var> for robot 1<br>
		    <span class="statement-lineno">Line 2:</span> <var>left_motor</var> <var>right_motor</var> <var>mechanical_order</var> for robot 2<br>
		    <span class="statement-lineno">Line 3:</span> <var>estimated_points</var><br>
		    <ul>
		      	<li><var>left_motor</var> and <var>right_motor</var> must be signed integer from -100 to 100</li>
		      	<li><var>mechanical_order</var> can be :
		      		<ul>
		      		<li><b>IDLE</b>: do nothing</li>
		      		<li><b>ACTIVATE_FRONT</b>: activate the front actuator</li>		<li><b>ACTIVATE_LEFT</b>: activate the left actuator</li>		<li><b>ACTIVATE_RIGHT</b>: activate the right actuator</li>
		      		<li><b>TAKE</b>: take playing element (actuator must be activated before)</li>
		      		<li><b>RELEASE</b>: release playing element (actuator must be activated before)</li>
		      		<li><b>RELEASE</b>: release playing element (actuator must be activated before)</li>
		      		<li><b>LIGHT</b>: turn the lighthouse on (actuator must be activated before)</li>
		      		<li><b>WIND</b>: turn the windsock on (actuator must be activated before)</li>

		      		<li><b>FLAG</b>: show the flag (only after 95 s)</li>
		      		</ul>
		      	</li>
		      	<li><var>estimated_points</var> is an unsigned integer for bonus calculation</li>
		  	</ul>
		</p>
      </div>
    </div>

    <!-- Protocol block -->
    <div class="blk">
      <div class="title">Constraints</div>
      <div class="text">Response time for first turn ≤ 1000ms <br>
        Response time for one turn ≤ 50ms</div>
    </div>
  </div>
</div>