package com.cakesportal.hustlerush;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class HustleRushView extends View {
    private enum Screen { MENU, HOW_TO, PRIVACY, PLAYING, PAUSED, GAME_OVER }
    private enum EntityType { CASH, BILL, SHIELD }

    private static final int STARTING_CASH = 300;
    private static final int MAX_COMBO = 5;
    private static final String PREFS = "hustle_rush_prefs";
    private static final String PREF_BEST = "best_cash";
    private static final String PREF_SOUND = "sound_enabled";
    private static final String PREF_HAPTIC = "haptic_enabled";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random random = new Random();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final SharedPreferences preferences;
    private final Vibrator vibrator;
    private final ToneGenerator toneGenerator;

    private final RectF primaryButton = new RectF();
    private final RectF secondaryLeftButton = new RectF();
    private final RectF secondaryRightButton = new RectF();
    private final RectF soundButton = new RectF();
    private final RectF hapticButton = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF homeButton = new RectF();

    private Screen screen = Screen.MENU;
    private int topInset;
    private int bottomInset;
    private float density;
    private float width;
    private float height;
    private long lastFrameNanos;
    private float ambientTime;
    private float roadOffset;
    private float shakeTime;
    private float comboTimer;
    private float spawnTimer;
    private float distance;
    private float speed;
    private float touchDownX;
    private float touchDownY;
    private float playerX;
    private float playerTargetX;
    private float playerY;
    private int playerLane = 1;
    private int cash = STARTING_CASH;
    private int runPeak = STARTING_CASH;
    private int allTimeBest;
    private int combo = 1;
    private int runBestCombo = 1;
    private int collectedCount;
    private int billsPaid;
    private int shieldCharges;
    private boolean soundEnabled;
    private boolean hapticEnabled;
    private boolean firstFrame = true;

    private final int backgroundTop = Color.rgb(15, 17, 41);
    private final int backgroundBottom = Color.rgb(5, 8, 20);
    private final int panel = Color.rgb(24, 27, 55);
    private final int panelLight = Color.rgb(34, 38, 72);
    private final int accent = Color.rgb(140, 124, 255);
    private final int accentLight = Color.rgb(181, 170, 255);
    private final int cashGreen = Color.rgb(50, 213, 154);
    private final int danger = Color.rgb(255, 100, 121);
    private final int warning = Color.rgb(255, 190, 92);
    private final int cyan = Color.rgb(66, 210, 255);
    private final int textPrimary = Color.rgb(247, 247, 255);
    private final int textMuted = Color.rgb(167, 171, 199);
    private final int road = Color.rgb(22, 25, 46);
    private final int roadLine = Color.rgb(80, 85, 122);

    public HustleRushView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        allTimeBest = preferences.getInt(PREF_BEST, 0);
        soundEnabled = preferences.getBoolean(PREF_SOUND, true);
        hapticEnabled = preferences.getBoolean(PREF_HAPTIC, true);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        setFocusable(true);
        setKeepScreenOn(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                topInset = bars.top;
                bottomInset = bars.bottom;
            } else {
                topInset = insets.getStableInsetTop();
                bottomInset = insets.getStableInsetBottom();
            }
            requestLayout();
            return insets;
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        updatePlayerGeometry();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = firstFrame ? 0f : Math.min((now - lastFrameNanos) / 1_000_000_000f, 0.04f);
        firstFrame = false;
        lastFrameNanos = now;
        ambientTime += dt;

        if (screen == Screen.PLAYING) updateGame(dt);
        else updateAmbient(dt);

        canvas.save();
        if (shakeTime > 0f) {
            float amount = dp(4.5f) * Math.min(1f, shakeTime / 0.25f);
            canvas.translate((random.nextFloat() - 0.5f) * amount,
                    (random.nextFloat() - 0.5f) * amount);
        }

        drawBackground(canvas);
        switch (screen) {
            case MENU -> drawMenu(canvas);
            case HOW_TO -> drawHowTo(canvas);
            case PRIVACY -> drawPrivacy(canvas);
            case PLAYING -> drawGame(canvas, false);
            case PAUSED -> {
                drawGame(canvas, false);
                drawPauseOverlay(canvas);
            }
            case GAME_OVER -> {
                drawGame(canvas, true);
                drawGameOverOverlay(canvas);
            }
        }
        canvas.restore();

        if (screen == Screen.PLAYING || screen == Screen.MENU || !particles.isEmpty()) {
            postInvalidateOnAnimation();
        }
    }

    private void updateAmbient(float dt) {
        roadOffset = (roadOffset + dp(40f) * dt) % dp(90f);
        updateParticles(dt);
    }

    private void updateGame(float dt) {
        if (dt <= 0f) return;
        distance += dt * 20f;
        speed = dp(235f) + dp(distance * 0.15f);
        roadOffset = (roadOffset + speed * dt) % dp(90f);
        spawnTimer -= dt;
        comboTimer = Math.max(0f, comboTimer - dt);
        shakeTime = Math.max(0f, shakeTime - dt);

        if (comboTimer <= 0f && combo > 1) combo = 1;

        updatePlayerGeometry();
        playerX += (playerTargetX - playerX) * Math.min(1f, dt * 13f);

        if (spawnTimer <= 0f) {
            spawnPattern();
            float difficulty = Math.min(0.35f, distance / 2500f);
            spawnTimer = 0.86f - difficulty + random.nextFloat() * 0.14f;
        }

        for (Entity entity : entities) {
            entity.y += speed * dt;
            entity.rotation += dt * 2.2f;
            entity.x = laneCenter(entity.lane, entity.y);
        }

        RectF playerRect = getPlayerHitBox();
        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (RectF.intersects(playerRect, entity.hitBox(this))) {
                handleCollision(entity);
                iterator.remove();
                if (screen != Screen.PLAYING) break;
            } else if (entity.y > height + dp(100f)) {
                iterator.remove();
            }
        }

        updateParticles(dt);
    }

    private void updateParticles(float dt) {
        Iterator<Particle> particleIterator = particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy += dp(260f) * dt;
            particle.life -= dt;
            if (particle.life <= 0f) particleIterator.remove();
        }

        Iterator<FloatingText> textIterator = floatingTexts.iterator();
        while (textIterator.hasNext()) {
            FloatingText floatingText = textIterator.next();
            floatingText.y -= dp(44f) * dt;
            floatingText.life -= dt;
            if (floatingText.life <= 0f) textIterator.remove();
        }
    }

    private void spawnPattern() {
        int stage = (int) (distance / 450f);
        float moneyChance = Math.max(0.42f, 0.60f - distance / 7000f);
        boolean doublePattern = distance > 160f && random.nextFloat() < 0.24f;

        if (doublePattern) {
            int safeLane = random.nextInt(3);
            for (int lane = 0; lane < 3; lane++) {
                if (lane == safeLane) continue;
                if (random.nextFloat() < 0.62f) addBill(lane, stage, lane == 2 ? dp(-22f) : 0f);
                else addCash(lane, stage, lane == 2 ? dp(-22f) : 0f);
            }
            if (random.nextFloat() < 0.72f) addCash(safeLane, stage, dp(-48f));
            return;
        }

        int lane = random.nextInt(3);
        float roll = random.nextFloat();
        if (roll < 0.035f && distance > 120f) {
            entities.add(Entity.shield(lane, roadTop() - dp(48f)));
        } else if (roll < moneyChance) {
            addCash(lane, stage, 0f);
        } else {
            addBill(lane, stage, 0f);
        }
    }

    private void addCash(int lane, int stage, float yOffset) {
        int[] values = {50, 75, 100, 150, 200};
        int value = values[random.nextInt(values.length)] + Math.min(150, stage * 10);
        entities.add(Entity.cash(lane, roadTop() - dp(48f) + yOffset, value));
    }

    private void addBill(int lane, int stage, float yOffset) {
        String[] names = {"TAX", "RENT", "EMI", "FUEL", "FINE"};
        int[] bases = {100, 150, 125, 60, 75};
        int index = random.nextInt(names.length);
        int scaled = bases[index] + Math.min(225, stage * 15);
        entities.add(Entity.bill(lane, roadTop() - dp(52f) + yOffset, names[index], scaled));
    }

    private void handleCollision(Entity entity) {
        if (entity.type == EntityType.CASH) {
            comboTimer = 2.35f;
            combo = Math.min(MAX_COMBO, combo + 1);
            runBestCombo = Math.max(runBestCombo, combo);
            int earned = entity.value * combo;
            cash += earned;
            runPeak = Math.max(runPeak, cash);
            collectedCount++;
            addBurst(entity.x, entity.y, cashGreen, 13);
            addFloatingText("+₹" + earned + "  x" + combo, entity.x, entity.y, cashGreen);
            playPickupSound();
            vibrate(18, 60);
        } else if (entity.type == EntityType.SHIELD) {
            shieldCharges = Math.min(2, shieldCharges + 1);
            addBurst(entity.x, entity.y, cyan, 16);
            addFloatingText("SHIELD +1", entity.x, entity.y, cyan);
            playShieldSound();
            vibrate(35, 90);
        } else {
            if (shieldCharges > 0) {
                shieldCharges--;
                addBurst(entity.x, entity.y, cyan, 18);
                addFloatingText("BLOCKED " + entity.name, entity.x, entity.y, cyan);
                playShieldSound();
                vibrate(45, 110);
                return;
            }

            cash -= entity.value;
            billsPaid++;
            combo = 1;
            comboTimer = 0f;
            shakeTime = 0.28f;
            addBurst(entity.x, entity.y, danger, 17);
            addFloatingText("-₹" + entity.value + "  " + entity.name, entity.x, entity.y, danger);
            playBillSound();
            vibrate(80, 170);

            if (cash <= 0) finishRun();
        }
    }

    private void finishRun() {
        cash = 0;
        screen = Screen.GAME_OVER;
        setKeepScreenOn(false);
        if (runPeak > allTimeBest) {
            allTimeBest = runPeak;
            preferences.edit().putInt(PREF_BEST, allTimeBest).apply();
        }
        playGameOverSound();
        vibrate(180, 210);
        invalidate();
    }

    private void startRun() {
        entities.clear();
        particles.clear();
        floatingTexts.clear();
        cash = STARTING_CASH;
        runPeak = STARTING_CASH;
        distance = 0f;
        combo = 1;
        runBestCombo = 1;
        comboTimer = 0f;
        collectedCount = 0;
        billsPaid = 0;
        shieldCharges = 0;
        spawnTimer = 0.45f;
        speed = dp(235f);
        shakeTime = 0f;
        playerLane = 1;
        updatePlayerGeometry();
        playerX = laneCenter(playerLane, playerY);
        playerTargetX = playerX;
        screen = Screen.PLAYING;
        setKeepScreenOn(true);
        firstFrame = true;
        invalidate();
    }

    private void updatePlayerGeometry() {
        playerY = height - bottomInset - dp(145f);
        playerTargetX = laneCenter(playerLane, playerY);
        if (playerX == 0f) playerX = playerTargetX;
    }

    private void changeLane(int direction) {
        if (screen != Screen.PLAYING) return;
        int next = Math.max(0, Math.min(2, playerLane + direction));
        if (next != playerLane) {
            playerLane = next;
            playerTargetX = laneCenter(playerLane, playerY);
            vibrate(12, 45);
        }
    }

    private float roadTop() {
        return topInset + dp(126f);
    }

    private float roadBottom() {
        return height - bottomInset + dp(20f);
    }

    private float roadLeft(float y) {
        float t = clamp((y - roadTop()) / Math.max(1f, roadBottom() - roadTop()), 0f, 1f);
        return lerp(width * 0.27f, width * 0.02f, t);
    }

    private float roadRight(float y) {
        float t = clamp((y - roadTop()) / Math.max(1f, roadBottom() - roadTop()), 0f, 1f);
        return lerp(width * 0.73f, width * 0.98f, t);
    }

    private float laneCenter(int lane, float y) {
        float left = roadLeft(y);
        float right = roadRight(y);
        float laneWidth = (right - left) / 3f;
        return left + laneWidth * (lane + 0.5f);
    }

    private float perspectiveScale(float y) {
        float t = clamp((y - roadTop()) / Math.max(1f, roadBottom() - roadTop()), 0f, 1f);
        return lerp(0.62f, 1.14f, t);
    }

    private RectF getPlayerHitBox() {
        float w = dp(42f);
        float h = dp(61f);
        return new RectF(playerX - w / 2f, playerY - h / 2f,
                playerX + w / 2f, playerY + h / 2f);
    }

    private void drawBackground(Canvas canvas) {
        LinearGradient gradient = new LinearGradient(0, 0, 0, height,
                backgroundTop, backgroundBottom, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        drawStars(canvas);
        drawMoon(canvas);
        drawSkyline(canvas);
    }

    private void drawStars(Canvas canvas) {
        paint.setColor(Color.argb(90, 220, 225, 255));
        for (int i = 0; i < 24; i++) {
            float x = ((i * 79f) % Math.max(1f, width));
            float y = topInset + dp(12f) + ((i * 47f) % dp(160f));
            float pulse = 0.55f + 0.45f * (float) Math.sin(ambientTime * 1.2f + i);
            paint.setAlpha((int) (50 + 90 * pulse));
            canvas.drawCircle(x, y, dp(i % 3 == 0 ? 1.4f : 0.9f), paint);
        }
        paint.setAlpha(255);
    }

    private void drawMoon(Canvas canvas) {
        float x = width * 0.82f;
        float y = topInset + dp(76f);
        paint.setColor(Color.argb(34, 140, 124, 255));
        canvas.drawCircle(x, y, dp(36f), paint);
        paint.setColor(Color.rgb(225, 224, 250));
        canvas.drawCircle(x, y, dp(19f), paint);
        paint.setColor(backgroundTop);
        canvas.drawCircle(x + dp(8f), y - dp(5f), dp(18f), paint);
    }

    private void drawSkyline(Canvas canvas) {
        float baseY = roadTop() + dp(10f);
        for (int i = 0; i < 13; i++) {
            float buildingW = width / 11f + dp((i % 3) * 4f);
            float x = i * width / 11f - dp(14f);
            float buildingH = dp(40f + (i * 19 % 92));
            paint.setColor(i % 2 == 0 ? Color.rgb(24, 27, 55) : Color.rgb(29, 32, 63));
            canvas.drawRect(x, baseY - buildingH, x + buildingW, baseY, paint);

            paint.setColor(Color.argb(95, 255, 206, 108));
            int rows = Math.max(1, (int) (buildingH / dp(22f)));
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < 2; col++) {
                    if ((row + col + i) % 3 == 0) continue;
                    float wx = x + dp(8f) + col * dp(15f);
                    float wy = baseY - buildingH + dp(10f) + row * dp(20f);
                    canvas.drawRoundRect(wx, wy, wx + dp(5f), wy + dp(8f), dp(1.5f), dp(1.5f), paint);
                }
            }
        }
    }

    private void drawRoad(Canvas canvas) {
        float top = roadTop();
        float bottom = roadBottom();
        path.reset();
        path.moveTo(roadLeft(top), top);
        path.lineTo(roadRight(top), top);
        path.lineTo(roadRight(bottom), bottom);
        path.lineTo(roadLeft(bottom), bottom);
        path.close();
        paint.setColor(road);
        canvas.drawPath(path, paint);

        strokePaint.setColor(Color.rgb(57, 61, 93));
        strokePaint.setStrokeWidth(dp(2.5f));
        canvas.drawPath(path, strokePaint);

        drawRoadGlow(canvas, top, bottom);
        drawLaneMarkers(canvas, top, bottom);
        drawRoadSideLights(canvas, top, bottom);
    }

    private void drawRoadGlow(Canvas canvas, float top, float bottom) {
        float center = laneCenter(playerLane, playerY);
        float laneWidthBottom = (roadRight(bottom) - roadLeft(bottom)) / 3f;
        float laneWidthTop = (roadRight(top) - roadLeft(top)) / 3f;
        float topCenter = laneCenter(playerLane, top);
        path.reset();
        path.moveTo(topCenter - laneWidthTop * 0.42f, top);
        path.lineTo(topCenter + laneWidthTop * 0.42f, top);
        path.lineTo(center + laneWidthBottom * 0.44f, bottom);
        path.lineTo(center - laneWidthBottom * 0.44f, bottom);
        path.close();
        paint.setColor(Color.argb(24, 140, 124, 255));
        canvas.drawPath(path, paint);
    }

    private void drawLaneMarkers(Canvas canvas, float top, float bottom) {
        strokePaint.setColor(roadLine);
        strokePaint.setStrokeWidth(dp(2.2f));
        for (int divider = 1; divider <= 2; divider++) {
            for (float y = top - roadOffset; y < bottom; y += dp(82f)) {
                float y1 = Math.max(top, y);
                float y2 = Math.min(bottom, y + dp(38f));
                if (y2 <= top) continue;
                float left1 = roadLeft(y1);
                float right1 = roadRight(y1);
                float left2 = roadLeft(y2);
                float right2 = roadRight(y2);
                float x1 = left1 + (right1 - left1) * divider / 3f;
                float x2 = left2 + (right2 - left2) * divider / 3f;
                canvas.drawLine(x1, y1, x2, y2, strokePaint);
            }
        }
    }

    private void drawRoadSideLights(Canvas canvas, float top, float bottom) {
        for (float y = top + ((roadOffset * 1.3f) % dp(70f)); y < bottom; y += dp(70f)) {
            float scale = perspectiveScale(y);
            float radius = dp(2.2f) * scale;
            paint.setColor(Color.argb(160, 66, 210, 255));
            canvas.drawCircle(roadLeft(y) - dp(7f) * scale, y, radius, paint);
            paint.setColor(Color.argb(160, 255, 100, 121));
            canvas.drawCircle(roadRight(y) + dp(7f) * scale, y, radius, paint);
        }
    }

    private void drawMenu(Canvas canvas) {
        float safeTop = topInset + dp(40f);
        drawLogo(canvas, width / 2f, safeTop + dp(55f), 1.12f);
        drawCenteredText(canvas, "HUSTLE RUSH", width / 2f, safeTop + dp(132f), dp(32f), textPrimary, true);
        drawCenteredText(canvas, "Earn fast. Survive every expense.", width / 2f, safeTop + dp(161f), dp(14f), textMuted, false);

        float cardTop = safeTop + dp(196f);
        RectF scoreCard = new RectF(dp(22f), cardTop, width - dp(22f), cardTop + dp(92f));
        drawPanel(canvas, scoreCard, dp(24f), panel, Color.argb(70, 140, 124, 255));
        drawText(canvas, "ALL-TIME BEST CASH", scoreCard.left + dp(20f), scoreCard.top + dp(27f), dp(12f), textMuted, true, Paint.Align.LEFT);
        drawText(canvas, "₹" + allTimeBest, scoreCard.left + dp(20f), scoreCard.top + dp(66f), dp(29f), textPrimary, true, Paint.Align.LEFT);
        drawMiniTrend(canvas, scoreCard.right - dp(76f), scoreCard.centerY());

        float buttonTop = cardTop + dp(118f);
        primaryButton.set(dp(22f), buttonTop, width - dp(22f), buttonTop + dp(62f));
        drawGradientButton(canvas, primaryButton, "START CITY RUN", "▶", accent, accentLight);

        secondaryLeftButton.set(dp(22f), buttonTop + dp(78f), width / 2f - dp(7f), buttonTop + dp(132f));
        secondaryRightButton.set(width / 2f + dp(7f), buttonTop + dp(78f), width - dp(22f), buttonTop + dp(132f));
        drawOutlineButton(canvas, secondaryLeftButton, "HOW TO PLAY", "?");
        drawOutlineButton(canvas, secondaryRightButton, "PRIVACY", "i");

        float toggleTop = buttonTop + dp(151f);
        soundButton.set(dp(22f), toggleTop, width / 2f - dp(7f), toggleTop + dp(48f));
        hapticButton.set(width / 2f + dp(7f), toggleTop, width - dp(22f), toggleTop + dp(48f));
        drawToggle(canvas, soundButton, "SOUND", soundEnabled, "♪");
        drawToggle(canvas, hapticButton, "HAPTIC", hapticEnabled, "≈");

        float badgeY = Math.min(height - bottomInset - dp(34f), toggleTop + dp(82f));
        drawPill(canvas, width / 2f, badgeY, "OFFLINE  •  NO LOGIN  •  NO DATA COLLECTION");
    }

    private void drawGame(Canvas canvas, boolean frozen) {
        drawRoad(canvas);
        for (Entity entity : entities) drawEntity(canvas, entity);
        drawParticles(canvas);
        drawPlayer(canvas);
        drawGameHud(canvas);
        if (frozen) {
            paint.setColor(Color.argb(70, 5, 8, 20));
            canvas.drawRect(0, 0, width, height, paint);
        }
    }

    private void drawGameHud(Canvas canvas) {
        float top = topInset + dp(14f);
        float side = dp(14f);
        float gap = dp(8f);
        float pauseSize = dp(48f);
        float cardWidth = (width - side * 2f - gap * 2f - pauseSize) / 3f;

        RectF wallet = new RectF(side, top, side + cardWidth, top + dp(58f));
        RectF peak = new RectF(wallet.right + gap, top, wallet.right + gap + cardWidth, top + dp(58f));
        RectF comboCard = new RectF(peak.right + gap, top, peak.right + gap + cardWidth, top + dp(58f));
        pauseButton.set(width - side - pauseSize, top + dp(5f), width - side, top + dp(5f) + pauseSize);

        drawHudCard(canvas, wallet, "WALLET", "₹" + Math.max(0, cash), cashGreen);
        drawHudCard(canvas, peak, "PEAK", "₹" + runPeak, accentLight);
        drawHudCard(canvas, comboCard, "COMBO", "x" + combo, warning);
        drawCircleButton(canvas, pauseButton, "Ⅱ");

        float infoY = top + dp(71f);
        drawSmallBadge(canvas, dp(14f), infoY, Math.round(distance) + " m", textPrimary);
        String shieldText = shieldCharges > 0 ? "SHIELD x" + shieldCharges : String.format(Locale.US, "SPEED %.1fx", speed / dp(235f));
        float badgeWidth = textWidth(shieldText, dp(11f), true) + dp(22f);
        drawSmallBadge(canvas, width - dp(14f) - badgeWidth, infoY, shieldText, shieldCharges > 0 ? cyan : textPrimary);
    }

    private void drawEntity(Canvas canvas, Entity entity) {
        float scale = perspectiveScale(entity.y);
        canvas.save();
        canvas.translate(entity.x, entity.y);
        canvas.rotate((float) Math.sin(entity.rotation) * (entity.type == EntityType.BILL ? 2.5f : 5f));
        canvas.scale(scale, scale);

        if (entity.type == EntityType.CASH) {
            drawCashBundle(canvas, entity.value);
        } else if (entity.type == EntityType.BILL) {
            drawBillCard(canvas, entity.name, entity.value);
        } else {
            drawShieldToken(canvas);
        }
        canvas.restore();
    }

    private void drawCashBundle(Canvas canvas, int value) {
        paint.setColor(Color.argb(70, 50, 213, 154));
        canvas.drawCircle(0, 0, dp(35f), paint);
        RectF shadow = new RectF(-dp(32f), -dp(20f), dp(32f), dp(23f));
        drawPanel(canvas, shadow, dp(11f), Color.rgb(21, 116, 88), Color.TRANSPARENT);
        RectF front = new RectF(-dp(30f), -dp(24f), dp(30f), dp(19f));
        drawPanel(canvas, front, dp(10f), cashGreen, Color.argb(130, 255, 255, 255));
        paint.setColor(Color.argb(75, 0, 0, 0));
        canvas.drawRect(-dp(4f), -dp(24f), dp(5f), dp(19f), paint);
        drawCenteredText(canvas, "+₹" + value, 0, dp(1f), dp(14f), Color.WHITE, true);
    }

    private void drawBillCard(Canvas canvas, String name, int value) {
        paint.setColor(Color.argb(60, 255, 100, 121));
        canvas.drawCircle(0, 0, dp(37f), paint);
        RectF card = new RectF(-dp(36f), -dp(29f), dp(36f), dp(29f));
        drawPanel(canvas, card, dp(12f), Color.rgb(62, 35, 55), Color.argb(150, 255, 100, 121));
        paint.setColor(danger);
        canvas.drawRoundRect(-dp(27f), -dp(21f), dp(27f), -dp(8f), dp(5f), dp(5f), paint);
        drawCenteredText(canvas, name, 0, -dp(11.5f), dp(11f), Color.WHITE, true);
        drawCenteredText(canvas, "-₹" + value, 0, dp(12f), dp(15f), textPrimary, true);
    }

    private void drawShieldToken(Canvas canvas) {
        paint.setColor(Color.argb(60, 66, 210, 255));
        canvas.drawCircle(0, 0, dp(39f), paint);
        paint.setColor(cyan);
        path.reset();
        path.moveTo(0, -dp(29f));
        path.lineTo(dp(25f), -dp(18f));
        path.lineTo(dp(20f), dp(15f));
        path.quadTo(0, dp(34f), -dp(20f), dp(15f));
        path.lineTo(-dp(25f), -dp(18f));
        path.close();
        canvas.drawPath(path, paint);
        drawCenteredText(canvas, "S", 0, dp(3f), dp(20f), Color.WHITE, true);
    }

    private void drawPlayer(Canvas canvas) {
        float bounce = (float) Math.sin(ambientTime * 11f) * dp(1.4f);
        canvas.save();
        canvas.translate(playerX, playerY + bounce);

        paint.setColor(Color.argb(85, 140, 124, 255));
        canvas.drawOval(-dp(33f), dp(31f), dp(33f), dp(46f), paint);

        if (shieldCharges > 0) {
            strokePaint.setColor(Color.argb(210, 66, 210, 255));
            strokePaint.setStrokeWidth(dp(4f));
            canvas.drawCircle(0, 0, dp(43f), strokePaint);
            paint.setColor(Color.argb(24, 66, 210, 255));
            canvas.drawCircle(0, 0, dp(42f), paint);
        }

        path.reset();
        path.moveTo(-dp(27f), dp(29f));
        path.lineTo(-dp(31f), -dp(5f));
        path.quadTo(-dp(28f), -dp(31f), 0, -dp(38f));
        path.quadTo(dp(28f), -dp(31f), dp(31f), -dp(5f));
        path.lineTo(dp(27f), dp(29f));
        path.close();
        LinearGradient carGradient = new LinearGradient(0, -dp(38f), 0, dp(29f), accentLight, accent, Shader.TileMode.CLAMP);
        paint.setShader(carGradient);
        canvas.drawPath(path, paint);
        paint.setShader(null);

        paint.setColor(Color.rgb(25, 29, 58));
        canvas.drawRoundRect(-dp(19f), -dp(24f), dp(19f), -dp(5f), dp(7f), dp(7f), paint);
        paint.setColor(Color.argb(130, 66, 210, 255));
        canvas.drawRoundRect(-dp(16f), -dp(21f), dp(16f), -dp(8f), dp(5f), dp(5f), paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(-dp(19f), dp(16f), dp(5f), paint);
        canvas.drawCircle(dp(19f), dp(16f), dp(5f), paint);
        paint.setColor(Color.rgb(255, 72, 105));
        canvas.drawRoundRect(-dp(18f), dp(25f), -dp(6f), dp(30f), dp(2f), dp(2f), paint);
        canvas.drawRoundRect(dp(6f), dp(25f), dp(18f), dp(30f), dp(2f), dp(2f), paint);

        drawCenteredText(canvas, "₹", 0, dp(10f), dp(17f), Color.WHITE, true);
        canvas.restore();
    }

    private void drawParticles(Canvas canvas) {
        for (Particle particle : particles) {
            paint.setColor(particle.color);
            paint.setAlpha((int) (255 * clamp(particle.life / particle.maxLife, 0f, 1f)));
            canvas.drawCircle(particle.x, particle.y, particle.size, paint);
        }
        paint.setAlpha(255);

        for (FloatingText floatingText : floatingTexts) {
            int alpha = (int) (255 * clamp(floatingText.life / floatingText.maxLife, 0f, 1f));
            paint.setAlpha(alpha);
            drawCenteredText(canvas, floatingText.text, floatingText.x, floatingText.y, dp(16f), floatingText.color, true);
            paint.setAlpha(255);
        }
    }

    private void drawPauseOverlay(Canvas canvas) {
        paint.setColor(Color.argb(205, 5, 8, 20));
        canvas.drawRect(0, 0, width, height, paint);
        float cardWidth = Math.min(width - dp(44f), dp(360f));
        float left = (width - cardWidth) / 2f;
        float top = height / 2f - dp(150f);
        RectF card = new RectF(left, top, left + cardWidth, top + dp(300f));
        drawPanel(canvas, card, dp(28f), panel, Color.argb(90, 140, 124, 255));
        drawCenteredText(canvas, "RUN PAUSED", width / 2f, top + dp(58f), dp(25f), textPrimary, true);
        drawCenteredText(canvas, "Your wallet is safe for now.", width / 2f, top + dp(89f), dp(13f), textMuted, false);

        primaryButton.set(left + dp(20f), top + dp(126f), card.right - dp(20f), top + dp(184f));
        homeButton.set(left + dp(20f), top + dp(201f), card.right - dp(20f), top + dp(255f));
        drawGradientButton(canvas, primaryButton, "RESUME RUN", "▶", accent, accentLight);
        drawOutlineButton(canvas, homeButton, "EXIT TO HOME", "⌂");
    }

    private void drawGameOverOverlay(Canvas canvas) {
        paint.setColor(Color.argb(212, 5, 8, 20));
        canvas.drawRect(0, 0, width, height, paint);
        float cardWidth = Math.min(width - dp(36f), dp(390f));
        float left = (width - cardWidth) / 2f;
        float top = topInset + dp(72f);
        float bottom = height - bottomInset - dp(30f);
        RectF card = new RectF(left, top, left + cardWidth, bottom);
        drawPanel(canvas, card, dp(30f), panel, Color.argb(110, 255, 100, 121));

        paint.setColor(Color.argb(42, 255, 100, 121));
        canvas.drawCircle(width / 2f, top + dp(62f), dp(44f), paint);
        paint.setColor(danger);
        canvas.drawCircle(width / 2f, top + dp(62f), dp(31f), paint);
        drawCenteredText(canvas, "₹0", width / 2f, top + dp(68f), dp(21f), Color.WHITE, true);

        String title = runPeak >= 2500 ? "CITY TYCOON" : runPeak >= 1200 ? "STRONG HUSTLE" : "WALLET EMPTY";
        String subtitle = runPeak >= 2500
                ? "You built serious wealth before the bills caught up."
                : runPeak >= 1200
                ? "A strong run, but expenses finally won."
                : "Your expenses grew faster than your income.";
        drawCenteredText(canvas, title, width / 2f, top + dp(135f), dp(25f), textPrimary, true);
        drawCenteredText(canvas, subtitle, width / 2f, top + dp(164f), dp(12.5f), textMuted, false);

        float statsTop = top + dp(190f);
        float gap = dp(8f);
        float statWidth = (cardWidth - dp(48f) - gap * 2f) / 3f;
        drawStatBox(canvas, new RectF(left + dp(16f), statsTop, left + dp(16f) + statWidth, statsTop + dp(78f)), "PEAK", "₹" + runPeak, cashGreen);
        drawStatBox(canvas, new RectF(left + dp(16f) + statWidth + gap, statsTop, left + dp(16f) + statWidth * 2f + gap, statsTop + dp(78f)), "DISTANCE", Math.round(distance) + "m", accentLight);
        drawStatBox(canvas, new RectF(left + dp(16f) + statWidth * 2f + gap * 2f, statsTop, card.right - dp(16f), statsTop + dp(78f)), "COMBO", "x" + runBestCombo, warning);

        float detailsY = statsTop + dp(105f);
        drawText(canvas, "Cash pickups", left + dp(28f), detailsY, dp(12.5f), textMuted, false, Paint.Align.LEFT);
        drawText(canvas, String.valueOf(collectedCount), card.right - dp(28f), detailsY, dp(13f), textPrimary, true, Paint.Align.RIGHT);
        drawText(canvas, "Bills paid", left + dp(28f), detailsY + dp(28f), dp(12.5f), textMuted, false, Paint.Align.LEFT);
        drawText(canvas, String.valueOf(billsPaid), card.right - dp(28f), detailsY + dp(28f), dp(13f), textPrimary, true, Paint.Align.RIGHT);
        drawText(canvas, "All-time best", left + dp(28f), detailsY + dp(56f), dp(12.5f), textMuted, false, Paint.Align.LEFT);
        drawText(canvas, "₹" + allTimeBest, card.right - dp(28f), detailsY + dp(56f), dp(13f), cashGreen, true, Paint.Align.RIGHT);

        float buttonsTop = Math.min(bottom - dp(136f), detailsY + dp(82f));
        primaryButton.set(left + dp(18f), buttonsTop, card.right - dp(18f), buttonsTop + dp(58f));
        homeButton.set(left + dp(18f), buttonsTop + dp(72f), card.right - dp(18f), buttonsTop + dp(124f));
        drawGradientButton(canvas, primaryButton, "RUN AGAIN", "↻", accent, accentLight);
        drawOutlineButton(canvas, homeButton, "BACK TO HOME", "⌂");
    }

    private void drawHowTo(Canvas canvas) {
        float top = topInset + dp(36f);
        drawBackHeader(canvas, "HOW TO PLAY", top);
        float y = top + dp(78f);
        drawInstructionRow(canvas, y, cashGreen, "+₹", "COLLECT INCOME", "Cash increases your wallet. Consecutive pickups build a multiplier.");
        y += dp(105f);
        drawInstructionRow(canvas, y, danger, "-₹", "PAY EXPENSES", "Tax, rent, EMI, fuel and fines deduct the amount printed on them.");
        y += dp(105f);
        drawInstructionRow(canvas, y, cyan, "S", "GRAB SHIELDS", "A shield blocks one bill completely. You can hold up to two.");
        y += dp(105f);
        drawInstructionRow(canvas, y, accent, "↔", "CHANGE LANES", "Tap left or right, or swipe across the road. The run speeds up over time.");

        RectF tipCard = new RectF(dp(22f), y + dp(90f), width - dp(22f), y + dp(174f));
        drawPanel(canvas, tipCard, dp(22f), panel, Color.argb(75, 255, 190, 92));
        drawText(canvas, "WIN CONDITION", tipCard.left + dp(18f), tipCard.top + dp(25f), dp(11f), warning, true, Paint.Align.LEFT);
        drawText(canvas, "There is no finish line. Your score is the highest cash reached before your wallet hits ₹0.", tipCard.left + dp(18f), tipCard.top + dp(49f), dp(12.5f), textPrimary, false, Paint.Align.LEFT, tipCard.width() - dp(36f));
    }

    private void drawPrivacy(Canvas canvas) {
        float top = topInset + dp(36f);
        drawBackHeader(canvas, "PRIVACY", top);
        RectF card = new RectF(dp(22f), top + dp(78f), width - dp(22f), height - bottomInset - dp(32f));
        drawPanel(canvas, card, dp(24f), panel, Color.argb(60, 140, 124, 255));
        float x = card.left + dp(20f);
        float y = card.top + dp(34f);
        drawText(canvas, "Hustle Rush stores only:", x, y, dp(15f), textPrimary, true, Paint.Align.LEFT);
        y += dp(34f);
        drawBullet(canvas, x, y, "Your highest score");
        y += dp(31f);
        drawBullet(canvas, x, y, "Sound and vibration settings");
        y += dp(48f);
        drawText(canvas, "The app does not use internet access, accounts, analytics, advertising SDKs, location, contacts, camera or personal information.", x, y, dp(13f), textMuted, false, Paint.Align.LEFT, card.width() - dp(40f));
        y += dp(108f);
        drawText(canvas, "For the Play Store", x, y, dp(14f), textPrimary, true, Paint.Align.LEFT);
        y += dp(29f);
        drawText(canvas, "A ready-to-host privacy-policy.html file is included in the project root. Publish that file on your website and paste its public URL into Play Console.", x, y, dp(13f), textMuted, false, Paint.Align.LEFT, card.width() - dp(40f));
        y += dp(106f);
        drawText(canvas, "Developer: Cakesportal", x, y, dp(12.5f), textMuted, false, Paint.Align.LEFT);
    }

    private void drawBackHeader(Canvas canvas, String title, float top) {
        homeButton.set(dp(18f), top, dp(66f), top + dp(48f));
        drawCircleButton(canvas, homeButton, "‹");
        drawText(canvas, title, dp(82f), top + dp(31f), dp(22f), textPrimary, true, Paint.Align.LEFT);
    }

    private void drawInstructionRow(Canvas canvas, float top, int color, String symbol, String title, String body) {
        RectF card = new RectF(dp(22f), top, width - dp(22f), top + dp(88f));
        drawPanel(canvas, card, dp(20f), panel, Color.argb(45, Color.red(color), Color.green(color), Color.blue(color)));
        paint.setColor(Color.argb(45, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawCircle(card.left + dp(43f), card.centerY(), dp(27f), paint);
        paint.setColor(color);
        canvas.drawCircle(card.left + dp(43f), card.centerY(), dp(20f), paint);
        drawCenteredText(canvas, symbol, card.left + dp(43f), card.centerY() + dp(5f), dp(15f), Color.WHITE, true);
        drawText(canvas, title, card.left + dp(82f), card.top + dp(28f), dp(13f), textPrimary, true, Paint.Align.LEFT);
        drawText(canvas, body, card.left + dp(82f), card.top + dp(51f), dp(11.7f), textMuted, false, Paint.Align.LEFT, card.width() - dp(99f));
    }

    private void drawLogo(Canvas canvas, float cx, float cy, float scale) {
        paint.setColor(Color.argb(40, 140, 124, 255));
        canvas.drawCircle(cx, cy, dp(51f) * scale, paint);
        paint.setColor(accent);
        canvas.drawCircle(cx, cy, dp(38f) * scale, paint);
        strokePaint.setColor(accentLight);
        strokePaint.setStrokeWidth(dp(2.5f));
        canvas.drawCircle(cx, cy, dp(38f) * scale, strokePaint);
        drawCenteredText(canvas, "₹", cx, cy + dp(12f) * scale, dp(42f) * scale, Color.WHITE, true);
    }

    private void drawMiniTrend(Canvas canvas, float cx, float cy) {
        strokePaint.setStrokeWidth(dp(3f));
        strokePaint.setColor(cashGreen);
        path.reset();
        path.moveTo(cx - dp(34f), cy + dp(15f));
        path.lineTo(cx - dp(20f), cy + dp(5f));
        path.lineTo(cx - dp(7f), cy + dp(11f));
        path.lineTo(cx + dp(7f), cy - dp(8f));
        path.lineTo(cx + dp(21f), cy - dp(2f));
        path.lineTo(cx + dp(35f), cy - dp(20f));
        canvas.drawPath(path, strokePaint);
        paint.setColor(cashGreen);
        canvas.drawCircle(cx + dp(35f), cy - dp(20f), dp(4f), paint);
    }

    private void drawHudCard(Canvas canvas, RectF rect, String label, String value, int valueColor) {
        drawPanel(canvas, rect, dp(14f), Color.argb(220, 24, 27, 55), Color.argb(65, 255, 255, 255));
        drawCenteredText(canvas, label, rect.centerX(), rect.top + dp(18f), dp(8.8f), textMuted, true);
        drawCenteredText(canvas, value, rect.centerX(), rect.top + dp(43f), dp(16f), valueColor, true);
    }

    private void drawStatBox(Canvas canvas, RectF rect, String label, String value, int valueColor) {
        drawPanel(canvas, rect, dp(16f), panelLight, Color.argb(45, 255, 255, 255));
        drawCenteredText(canvas, label, rect.centerX(), rect.top + dp(24f), dp(9.5f), textMuted, true);
        drawCenteredText(canvas, value, rect.centerX(), rect.top + dp(54f), dp(17f), valueColor, true);
    }

    private void drawPanel(Canvas canvas, RectF rect, float radius, int fill, int borderColor) {
        paint.setColor(fill);
        canvas.drawRoundRect(rect, radius, radius, paint);
        if (borderColor != Color.TRANSPARENT) {
            strokePaint.setColor(borderColor);
            strokePaint.setStrokeWidth(dp(1.2f));
            canvas.drawRoundRect(rect, radius, radius, strokePaint);
        }
    }

    private void drawGradientButton(Canvas canvas, RectF rect, String label, String icon, int startColor, int endColor) {
        LinearGradient gradient = new LinearGradient(rect.left, rect.top, rect.right, rect.bottom, startColor, endColor, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawRoundRect(rect, dp(18f), dp(18f), paint);
        paint.setShader(null);
        paint.setColor(Color.argb(38, 255, 255, 255));
        canvas.drawRoundRect(rect.left + dp(2f), rect.top + dp(2f), rect.right - dp(2f), rect.top + dp(18f), dp(16f), dp(16f), paint);
        drawText(canvas, icon, rect.left + dp(26f), rect.centerY() + dp(6f), dp(19f), Color.WHITE, true, Paint.Align.LEFT);
        drawCenteredText(canvas, label, rect.centerX() + dp(8f), rect.centerY() + dp(5f), dp(14f), Color.WHITE, true);
    }

    private void drawOutlineButton(Canvas canvas, RectF rect, String label, String icon) {
        drawPanel(canvas, rect, dp(16f), panel, Color.argb(68, 255, 255, 255));
        drawText(canvas, icon, rect.left + dp(18f), rect.centerY() + dp(5f), dp(16f), accentLight, true, Paint.Align.LEFT);
        drawCenteredText(canvas, label, rect.centerX() + dp(9f), rect.centerY() + dp(4f), dp(11.5f), textPrimary, true);
    }

    private void drawToggle(Canvas canvas, RectF rect, String label, boolean enabled, String icon) {
        int border = enabled ? Color.argb(125, 50, 213, 154) : Color.argb(60, 255, 255, 255);
        int fill = enabled ? Color.rgb(25, 50, 54) : panel;
        drawPanel(canvas, rect, dp(15f), fill, border);
        drawText(canvas, icon, rect.left + dp(17f), rect.centerY() + dp(5f), dp(16f), enabled ? cashGreen : textMuted, true, Paint.Align.LEFT);
        drawText(canvas, label, rect.left + dp(43f), rect.centerY() + dp(4f), dp(11.5f), textPrimary, true, Paint.Align.LEFT);
        drawText(canvas, enabled ? "ON" : "OFF", rect.right - dp(14f), rect.centerY() + dp(4f), dp(9.5f), enabled ? cashGreen : textMuted, true, Paint.Align.RIGHT);
    }

    private void drawCircleButton(Canvas canvas, RectF rect, String label) {
        paint.setColor(Color.argb(225, 24, 27, 55));
        canvas.drawOval(rect, paint);
        strokePaint.setColor(Color.argb(75, 255, 255, 255));
        strokePaint.setStrokeWidth(dp(1.2f));
        canvas.drawOval(rect, strokePaint);
        drawCenteredText(canvas, label, rect.centerX(), rect.centerY() + dp(5f), dp(18f), textPrimary, true);
    }

    private void drawSmallBadge(Canvas canvas, float left, float top, String label, int color) {
        float w = textWidth(label, dp(11f), true) + dp(22f);
        RectF rect = new RectF(left, top, left + w, top + dp(27f));
        drawPanel(canvas, rect, dp(14f), Color.argb(210, 24, 27, 55), Color.argb(55, 255, 255, 255));
        drawCenteredText(canvas, label, rect.centerX(), rect.centerY() + dp(3.5f), dp(11f), color, true);
    }

    private void drawPill(Canvas canvas, float cx, float cy, String label) {
        float w = textWidth(label, dp(9.3f), true) + dp(26f);
        RectF rect = new RectF(cx - w / 2f, cy - dp(15f), cx + w / 2f, cy + dp(15f));
        drawPanel(canvas, rect, dp(15f), Color.argb(130, 24, 27, 55), Color.argb(45, 255, 255, 255));
        drawCenteredText(canvas, label, cx, cy + dp(3.5f), dp(9.3f), textMuted, true);
    }

    private void drawBullet(Canvas canvas, float x, float y, String text) {
        paint.setColor(cashGreen);
        canvas.drawCircle(x + dp(5f), y - dp(4f), dp(3.5f), paint);
        drawText(canvas, text, x + dp(20f), y, dp(13f), textPrimary, false, Paint.Align.LEFT);
    }

    private void addBurst(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float velocity = dp(65f + random.nextFloat() * 105f);
            particles.add(new Particle(x, y,
                    (float) Math.cos(angle) * velocity,
                    (float) Math.sin(angle) * velocity - dp(45f),
                    dp(2.2f + random.nextFloat() * 3.8f),
                    0.5f + random.nextFloat() * 0.35f,
                    color));
        }
    }

    private void addFloatingText(String text, float x, float y, int color) {
        floatingTexts.add(new FloatingText(text, x, y - dp(34f), 0.95f, color));
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float baselineY, float size, int color, boolean bold) {
        drawText(canvas, text, x, baselineY, size, color, bold, Paint.Align.CENTER);
    }

    private void drawText(Canvas canvas, String text, float x, float baselineY, float size, int color, boolean bold, Paint.Align align) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTextAlign(align);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        canvas.drawText(text, x, baselineY, paint);
    }

    private void drawText(Canvas canvas, String text, float x, float topY, float size, int color,
                          boolean bold, Paint.Align align, float maxWidth) {
        paint.setTextSize(size);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(align);
        paint.setColor(color);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float y = topY;
        float lineHeight = size * 1.36f;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), x, y, paint);
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) canvas.drawText(line.toString(), x, y, paint);
    }

    private float textWidth(String text, float size, boolean bold) {
        paint.setTextSize(size);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
        return paint.measureText(text);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                touchDownX = x;
                touchDownY = y;
                return true;
            }
            case MotionEvent.ACTION_UP -> {
                handleTapOrSwipe(x, y);
                performClick();
                return true;
            }
            default -> { return true; }
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void handleTapOrSwipe(float x, float y) {
        float dx = x - touchDownX;
        float dy = y - touchDownY;
        boolean swipe = Math.abs(dx) > dp(28f) && Math.abs(dx) > Math.abs(dy);

        if (screen == Screen.PLAYING) {
            if (pauseButton.contains(x, y)) {
                screen = Screen.PAUSED;
                setKeepScreenOn(false);
                invalidate();
                return;
            }
            if (swipe) changeLane(dx > 0 ? 1 : -1);
            else changeLane(x < width / 2f ? -1 : 1);
            return;
        }

        if (screen == Screen.MENU) {
            if (primaryButton.contains(x, y)) startRun();
            else if (secondaryLeftButton.contains(x, y)) screen = Screen.HOW_TO;
            else if (secondaryRightButton.contains(x, y)) screen = Screen.PRIVACY;
            else if (soundButton.contains(x, y)) toggleSound();
            else if (hapticButton.contains(x, y)) toggleHaptic();
        } else if (screen == Screen.HOW_TO || screen == Screen.PRIVACY) {
            if (homeButton.contains(x, y)) screen = Screen.MENU;
        } else if (screen == Screen.PAUSED) {
            if (primaryButton.contains(x, y)) {
                screen = Screen.PLAYING;
                setKeepScreenOn(true);
                firstFrame = true;
            } else if (homeButton.contains(x, y)) goHome();
        } else if (screen == Screen.GAME_OVER) {
            if (primaryButton.contains(x, y)) startRun();
            else if (homeButton.contains(x, y)) goHome();
        }
        invalidate();
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        preferences.edit().putBoolean(PREF_SOUND, soundEnabled).apply();
        if (soundEnabled) playPickupSound();
    }

    private void toggleHaptic() {
        hapticEnabled = !hapticEnabled;
        preferences.edit().putBoolean(PREF_HAPTIC, hapticEnabled).apply();
        if (hapticEnabled) vibrate(35, 100);
    }

    private void goHome() {
        screen = Screen.MENU;
        entities.clear();
        particles.clear();
        floatingTexts.clear();
        setKeepScreenOn(false);
    }

    public void pauseFromActivity() {
        if (screen == Screen.PLAYING) {
            screen = Screen.PAUSED;
            setKeepScreenOn(false);
            invalidate();
        }
    }

    public boolean handleBackPressed() {
        if (screen == Screen.PLAYING) {
            screen = Screen.PAUSED;
            setKeepScreenOn(false);
            invalidate();
            return true;
        }
        if (screen == Screen.PAUSED || screen == Screen.GAME_OVER || screen == Screen.HOW_TO || screen == Screen.PRIVACY) {
            goHome();
            invalidate();
            return true;
        }
        return false;
    }

    private void playPickupSound() {
        if (soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 55);
    }

    private void playBillSound() {
        if (soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 110);
    }

    private void playShieldSound() {
        if (soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 90);
    }

    private void playGameOverSound() {
        if (soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 260);
    }

    private void vibrate(long milliseconds, int amplitude) {
        if (!hapticEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, Math.min(255, amplitude)));
        } else {
            //noinspection deprecation
            vibrator.vibrate(milliseconds);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        toneGenerator.release();
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return value * density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static final class Entity {
        final EntityType type;
        final int lane;
        final String name;
        final int value;
        float x;
        float y;
        float rotation;

        private Entity(EntityType type, int lane, float y, String name, int value) {
            this.type = type;
            this.lane = lane;
            this.y = y;
            this.name = name;
            this.value = value;
        }

        static Entity cash(int lane, float y, int value) {
            return new Entity(EntityType.CASH, lane, y, "CASH", value);
        }

        static Entity bill(int lane, float y, String name, int value) {
            return new Entity(EntityType.BILL, lane, y, name, value);
        }

        static Entity shield(int lane, float y) {
            return new Entity(EntityType.SHIELD, lane, y, "SHIELD", 0);
        }

        RectF hitBox(HustleRushView view) {
            float scale = view.perspectiveScale(y);
            float w = view.dp(type == EntityType.BILL ? 54f : 47f) * scale;
            float h = view.dp(type == EntityType.BILL ? 48f : 44f) * scale;
            return new RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f);
        }
    }

    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        final float size;
        float life;
        final float maxLife;
        final int color;

        Particle(float x, float y, float vx, float vy, float size, float life, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }

    private static final class FloatingText {
        final String text;
        final float x;
        float y;
        float life;
        final float maxLife;
        final int color;

        FloatingText(String text, float x, float y, float life, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }
}
