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
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class HustleRushView extends View {
    private static final int MENU = 0;
    private static final int PLAYING = 1;
    private static final int PAUSED = 2;
    private static final int GAME_OVER = 3;

    private static final int CASH = 0;
    private static final int BILL = 1;
    private static final int STARTING_CASH = 300;
    private static final String PREFS = "hustle_rush_prefs";
    private static final String PREF_BEST = "best_cash";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random random = new Random();
    private final List<Entity> entities = new ArrayList<>();
    private final SharedPreferences preferences;

    private final RectF startButton = new RectF();
    private final RectF pauseButton = new RectF();
    private final RectF primaryButton = new RectF();
    private final RectF homeButton = new RectF();

    private int screen = MENU;
    private int wallet = STARTING_CASH;
    private int runPeak = STARTING_CASH;
    private int allTimeBest;
    private int combo = 1;
    private int bestCombo = 1;
    private int playerLane = 1;

    private float density;
    private float width;
    private float height;
    private float playerX;
    private float playerTargetX;
    private float playerY;
    private float distance;
    private float speed;
    private float spawnTimer;
    private float roadOffset;
    private float comboTimer;
    private float flashTimer;
    private float touchDownX;
    private long lastFrameNanos;
    private boolean firstFrame = true;

    private String eventText = "";
    private int eventColor = Color.WHITE;
    private float eventTimer;

    private final int backgroundTop = Color.rgb(15, 17, 41);
    private final int backgroundBottom = Color.rgb(5, 8, 20);
    private final int panel = Color.rgb(24, 27, 55);
    private final int panelLight = Color.rgb(35, 39, 75);
    private final int road = Color.rgb(25, 28, 51);
    private final int roadLine = Color.rgb(91, 96, 135);
    private final int accent = Color.rgb(140, 124, 255);
    private final int accentLight = Color.rgb(186, 176, 255);
    private final int moneyGreen = Color.rgb(52, 214, 154);
    private final int danger = Color.rgb(255, 96, 121);
    private final int warning = Color.rgb(255, 190, 92);
    private final int textPrimary = Color.rgb(247, 247, 255);
    private final int textMuted = Color.rgb(172, 176, 204);

    public HustleRushView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        allTimeBest = preferences.getInt(PREF_BEST, 0);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        setFocusable(true);
        setClickable(true);
        setKeepScreenOn(false);
        setBackgroundColor(backgroundBottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        playerY = height * 0.79f;
        playerX = laneCenter(playerLane);
        playerTargetX = playerX;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.nanoTime();
        float dt = firstFrame ? 0f : Math.min(0.04f, (now - lastFrameNanos) / 1_000_000_000f);
        firstFrame = false;
        lastFrameNanos = now;

        if (screen == PLAYING) updateGame(dt);
        else roadOffset = (roadOffset + dp(35f) * dt) % dp(90f);

        drawBackground(canvas);
        if (screen == MENU) {
            drawMenu(canvas);
        } else {
            drawGame(canvas);
            if (screen == PAUSED) drawPaused(canvas);
            if (screen == GAME_OVER) drawGameOver(canvas);
        }

        if (screen == PLAYING || screen == MENU || eventTimer > 0f) {
            postInvalidateOnAnimation();
        }
    }

    private void updateGame(float dt) {
        if (dt <= 0f) return;

        distance += dt * 20f;
        speed = dp(240f + Math.min(230f, distance * 0.18f));
        roadOffset = (roadOffset + speed * dt) % dp(90f);
        spawnTimer -= dt;
        comboTimer = Math.max(0f, comboTimer - dt);
        eventTimer = Math.max(0f, eventTimer - dt);
        flashTimer = Math.max(0f, flashTimer - dt);

        if (comboTimer <= 0f && combo > 1) combo = 1;

        playerX += (playerTargetX - playerX) * Math.min(1f, dt * 14f);

        if (spawnTimer <= 0f) {
            spawnEntity();
            float difficulty = Math.min(0.32f, distance / 2800f);
            spawnTimer = 0.84f - difficulty + random.nextFloat() * 0.13f;
        }

        for (Entity entity : entities) {
            entity.y += speed * dt;
        }

        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity.lane == playerLane && Math.abs(entity.y - playerY) < dp(54f)) {
                handleCollision(entity);
                iterator.remove();
                if (screen != PLAYING) break;
            } else if (entity.y > height + dp(80f)) {
                iterator.remove();
            }
        }
    }

    private void spawnEntity() {
        int lane = random.nextInt(3);
        float moneyChance = Math.max(0.43f, 0.61f - distance / 8000f);

        if (random.nextFloat() < moneyChance) {
            int[] values = {50, 75, 100, 150, 200};
            int value = values[random.nextInt(values.length)];
            entities.add(new Entity(CASH, lane, roadTop() - dp(55f), value, "CASH"));
        } else {
            String[] names = {"TAX", "RENT", "EMI", "FUEL", "FINE"};
            int[] values = {100, 150, 125, 60, 75};
            int index = random.nextInt(names.length);
            int stageIncrease = Math.min(200, ((int) distance / 500) * 10);
            entities.add(new Entity(BILL, lane, roadTop() - dp(55f),
                    values[index] + stageIncrease, names[index]));
        }
    }

    private void handleCollision(Entity entity) {
        if (entity.type == CASH) {
            combo = Math.min(5, combo + 1);
            bestCombo = Math.max(bestCombo, combo);
            comboTimer = 2.25f;
            int earned = entity.value * combo;
            wallet += earned;
            runPeak = Math.max(runPeak, wallet);
            showEvent("+₹" + earned + "   x" + combo, moneyGreen);
        } else {
            wallet -= entity.value;
            combo = 1;
            comboTimer = 0f;
            flashTimer = 0.22f;
            showEvent("-₹" + entity.value + "   " + entity.label, danger);
            if (wallet <= 0) finishRun();
        }
    }

    private void showEvent(String text, int color) {
        eventText = text;
        eventColor = color;
        eventTimer = 0.9f;
    }

    private void startRun() {
        screen = PLAYING;
        wallet = STARTING_CASH;
        runPeak = STARTING_CASH;
        combo = 1;
        bestCombo = 1;
        distance = 0f;
        spawnTimer = 0.35f;
        entities.clear();
        playerLane = 1;
        playerX = laneCenter(playerLane);
        playerTargetX = playerX;
        eventText = "";
        eventTimer = 0f;
        firstFrame = true;
        setKeepScreenOn(true);
        invalidate();
    }

    private void finishRun() {
        wallet = 0;
        screen = GAME_OVER;
        setKeepScreenOn(false);
        if (runPeak > allTimeBest) {
            allTimeBest = runPeak;
            preferences.edit().putInt(PREF_BEST, allTimeBest).apply();
        }
        invalidate();
    }

    private void moveLane(int direction) {
        if (screen != PLAYING) return;
        playerLane = Math.max(0, Math.min(2, playerLane + direction));
        playerTargetX = laneCenter(playerLane);
    }

    private void drawBackground(Canvas canvas) {
        paint.setShader(new LinearGradient(0f, 0f, 0f, height,
                flashTimer > 0f ? Color.rgb(51, 20, 38) : backgroundTop,
                backgroundBottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, height, paint);
        paint.setShader(null);

        drawSkyline(canvas);
        drawRoad(canvas);
    }

    private void drawSkyline(Canvas canvas) {
        float horizon = roadTop() + dp(10f);
        paint.setColor(panel);
        for (int i = 0; i < 12; i++) {
            float buildingWidth = width / 11f;
            float left = i * buildingWidth - dp(8f);
            float buildingHeight = dp(35f + ((i * 31) % 90));
            canvas.drawRect(left, horizon - buildingHeight,
                    left + buildingWidth * 0.78f, horizon, paint);

            paint.setColor(Color.rgb(73, 77, 113));
            for (int row = 0; row < 3; row++) {
                float windowY = horizon - buildingHeight + dp(10f + row * 18f);
                canvas.drawRect(left + dp(7f), windowY,
                        left + dp(12f), windowY + dp(7f), paint);
            }
            paint.setColor(panel);
        }
    }

    private void drawRoad(Canvas canvas) {
        float top = roadTop();
        float bottom = height;
        float topLeft = width * 0.29f;
        float topRight = width * 0.71f;
        float bottomLeft = width * 0.04f;
        float bottomRight = width * 0.96f;

        path.reset();
        path.moveTo(topLeft, top);
        path.lineTo(topRight, top);
        path.lineTo(bottomRight, bottom);
        path.lineTo(bottomLeft, bottom);
        path.close();
        paint.setColor(road);
        canvas.drawPath(path, paint);

        stroke.setColor(roadLine);
        stroke.setStrokeWidth(dp(2f));
        for (int lane = 1; lane <= 2; lane++) {
            float topX = topLeft + (topRight - topLeft) * lane / 3f;
            float bottomX = bottomLeft + (bottomRight - bottomLeft) * lane / 3f;
            for (float y = top - roadOffset; y < bottom; y += dp(90f)) {
                float y2 = Math.min(bottom, y + dp(42f));
                if (y2 < top) continue;
                float t1 = clamp((y - top) / Math.max(1f, bottom - top), 0f, 1f);
                float t2 = clamp((y2 - top) / Math.max(1f, bottom - top), 0f, 1f);
                float x1 = lerp(topX, bottomX, t1);
                float x2 = lerp(topX, bottomX, t2);
                canvas.drawLine(x1, Math.max(top, y), x2, y2, stroke);
            }
        }
    }

    private void drawMenu(Canvas canvas) {
        float center = width / 2f;
        float titleY = Math.max(dp(115f), height * 0.18f);

        drawCenteredText(canvas, "HUSTLE", center, titleY, sp(34f), textPrimary, true);
        drawCenteredText(canvas, "RUSH", center, titleY + dp(40f), sp(34f), accentLight, true);
        drawCenteredText(canvas, "Earn more than the city takes.", center,
                titleY + dp(76f), sp(15f), textMuted, false);

        float cardTop = titleY + dp(105f);
        float margin = dp(22f);
        drawRoundedPanel(canvas, margin, cardTop, width - margin,
                cardTop + dp(118f), dp(22f), panel);

        drawText(canvas, "HOW TO SURVIVE", margin + dp(18f), cardTop + dp(31f),
                sp(13f), accentLight, true, Paint.Align.LEFT);
        drawText(canvas, "Collect income to grow your wallet.", margin + dp(18f),
                cardTop + dp(61f), sp(14f), textPrimary, false, Paint.Align.LEFT);
        drawText(canvas, "Bills deduct their displayed value.", margin + dp(18f),
                cardTop + dp(87f), sp(14f), textPrimary, false, Paint.Align.LEFT);

        float statsTop = cardTop + dp(137f);
        drawMetric(canvas, margin, statsTop, (width - dp(50f)) / 2f,
                "START", "₹300", moneyGreen);
        drawMetric(canvas, (width + dp(6f)) / 2f, statsTop, width - margin,
                "BEST", "₹" + allTimeBest, accentLight);

        float buttonTop = Math.min(height - dp(105f), statsTop + dp(104f));
        startButton.set(margin, buttonTop, width - margin, buttonTop + dp(62f));
        drawButton(canvas, startButton, accent, "START CITY RUN");
    }

    private void drawGame(Canvas canvas) {
        for (Entity entity : entities) drawEntity(canvas, entity);
        drawPlayer(canvas);
        drawHud(canvas);

        if (eventTimer > 0f && !eventText.isEmpty()) {
            float alpha = Math.min(1f, eventTimer * 2f);
            paint.setAlpha((int) (255 * alpha));
            drawCenteredText(canvas, eventText, width / 2f, height * 0.24f,
                    sp(20f), eventColor, true);
            paint.setAlpha(255);
        }
    }

    private void drawHud(Canvas canvas) {
        float margin = dp(12f);
        float top = dp(14f);
        float gap = dp(7f);
        float cardWidth = (width - margin * 2f - gap * 2f) / 3f;

        drawHudCard(canvas, margin, top, cardWidth, "WALLET", "₹" + Math.max(0, wallet), moneyGreen);
        drawHudCard(canvas, margin + cardWidth + gap, top, cardWidth,
                "HIGHEST", "₹" + runPeak, accentLight);
        drawHudCard(canvas, margin + (cardWidth + gap) * 2f, top, cardWidth,
                "COMBO", "x" + combo, warning);

        pauseButton.set(width - dp(54f), top + dp(74f), width - dp(12f), top + dp(116f));
        drawRoundedPanel(canvas, pauseButton.left, pauseButton.top, pauseButton.right,
                pauseButton.bottom, dp(13f), panelLight);
        drawCenteredText(canvas, "Ⅱ", pauseButton.centerX(), pauseButton.centerY() + dp(7f),
                sp(18f), textPrimary, true);

        drawText(canvas, String.format(Locale.US, "%dm", (int) distance), dp(14f),
                top + dp(103f), sp(14f), textMuted, true, Paint.Align.LEFT);
    }

    private void drawHudCard(Canvas canvas, float left, float top, float cardWidth,
                             String label, String value, int valueColor) {
        drawRoundedPanel(canvas, left, top, left + cardWidth, top + dp(67f), dp(15f), panel);
        drawCenteredText(canvas, label, left + cardWidth / 2f, top + dp(23f),
                sp(10f), textMuted, true);
        drawCenteredText(canvas, value, left + cardWidth / 2f, top + dp(51f),
                sp(17f), valueColor, true);
    }

    private void drawEntity(Canvas canvas, Entity entity) {
        float x = laneCenter(entity.lane);
        float scale = 0.72f + 0.28f * clamp((entity.y - roadTop()) /
                Math.max(1f, height - roadTop()), 0f, 1f);
        float w = dp(entity.type == CASH ? 64f : 70f) * scale;
        float h = dp(entity.type == CASH ? 47f : 58f) * scale;
        RectF rect = new RectF(x - w / 2f, entity.y - h / 2f,
                x + w / 2f, entity.y + h / 2f);

        if (entity.type == CASH) {
            drawRoundedPanel(canvas, rect.left, rect.top, rect.right, rect.bottom,
                    dp(13f), moneyGreen);
            drawCenteredText(canvas, "+₹" + entity.value, x,
                    entity.y + sp(6f), sp(15f) * scale, Color.WHITE, true);
        } else {
            drawRoundedPanel(canvas, rect.left, rect.top, rect.right, rect.bottom,
                    dp(13f), panelLight);
            stroke.setColor(danger);
            stroke.setStrokeWidth(dp(2f));
            canvas.drawRoundRect(rect, dp(13f), dp(13f), stroke);
            drawCenteredText(canvas, entity.label, x, entity.y - dp(5f),
                    sp(12f) * scale, textPrimary, true);
            drawCenteredText(canvas, "-₹" + entity.value, x, entity.y + dp(15f),
                    sp(13f) * scale, danger, true);
        }
    }

    private void drawPlayer(Canvas canvas) {
        float w = dp(58f);
        float h = dp(76f);
        RectF car = new RectF(playerX - w / 2f, playerY - h / 2f,
                playerX + w / 2f, playerY + h / 2f);

        paint.setColor(Color.argb(70, 140, 124, 255));
        canvas.drawOval(new RectF(playerX - dp(38f), playerY + dp(29f),
                playerX + dp(38f), playerY + dp(45f)), paint);

        drawRoundedPanel(canvas, car.left, car.top, car.right, car.bottom,
                dp(18f), accent);
        drawRoundedPanel(canvas, playerX - dp(18f), playerY - dp(23f),
                playerX + dp(18f), playerY + dp(2f), dp(8f), Color.rgb(47, 49, 91));
        drawCenteredText(canvas, "₹", playerX, playerY + dp(26f),
                sp(23f), Color.WHITE, true);

        paint.setColor(accentLight);
        canvas.drawCircle(playerX - dp(18f), playerY + dp(20f), dp(5f), paint);
        canvas.drawCircle(playerX + dp(18f), playerY + dp(20f), dp(5f), paint);
    }

    private void drawPaused(Canvas canvas) {
        drawOverlay(canvas);
        float cardLeft = dp(28f);
        float cardRight = width - dp(28f);
        float cardTop = height * 0.29f;
        float cardBottom = cardTop + dp(260f);
        drawRoundedPanel(canvas, cardLeft, cardTop, cardRight, cardBottom, dp(24f), panel);

        drawCenteredText(canvas, "RUN PAUSED", width / 2f, cardTop + dp(58f),
                sp(24f), textPrimary, true);
        drawCenteredText(canvas, "Your wallet is safe for now.", width / 2f,
                cardTop + dp(91f), sp(14f), textMuted, false);

        primaryButton.set(cardLeft + dp(18f), cardTop + dp(125f),
                cardRight - dp(18f), cardTop + dp(183f));
        homeButton.set(cardLeft + dp(18f), cardTop + dp(194f),
                cardRight - dp(18f), cardTop + dp(242f));
        drawButton(canvas, primaryButton, accent, "CONTINUE");
        drawOutlineButton(canvas, homeButton, "END RUN");
    }

    private void drawGameOver(Canvas canvas) {
        drawOverlay(canvas);
        float cardLeft = dp(22f);
        float cardRight = width - dp(22f);
        float cardTop = height * 0.18f;
        float cardBottom = Math.min(height - dp(18f), cardTop + dp(440f));
        drawRoundedPanel(canvas, cardLeft, cardTop, cardRight, cardBottom, dp(25f), panel);

        drawCenteredText(canvas, "WALLET EMPTY", width / 2f, cardTop + dp(55f),
                sp(25f), danger, true);
        drawCenteredText(canvas, "The city finally caught up.", width / 2f,
                cardTop + dp(87f), sp(14f), textMuted, false);

        float metricTop = cardTop + dp(116f);
        drawMetric(canvas, cardLeft + dp(16f), metricTop, width / 2f - dp(5f),
                "PEAK CASH", "₹" + runPeak, moneyGreen);
        drawMetric(canvas, width / 2f + dp(5f), metricTop, cardRight - dp(16f),
                "DISTANCE", (int) distance + "m", accentLight);
        drawMetric(canvas, cardLeft + dp(16f), metricTop + dp(92f),
                cardRight - dp(16f), "BEST COMBO", "x" + bestCombo, warning);

        primaryButton.set(cardLeft + dp(16f), cardBottom - dp(126f),
                cardRight - dp(16f), cardBottom - dp(68f));
        homeButton.set(cardLeft + dp(16f), cardBottom - dp(57f),
                cardRight - dp(16f), cardBottom - dp(13f));
        drawButton(canvas, primaryButton, accent, "RUN AGAIN");
        drawOutlineButton(canvas, homeButton, "HOME");
    }

    private void drawOverlay(Canvas canvas) {
        paint.setColor(Color.argb(205, 5, 8, 20));
        canvas.drawRect(0f, 0f, width, height, paint);
    }

    private void drawMetric(Canvas canvas, float left, float top, float right,
                            String label, String value, int valueColor) {
        drawRoundedPanel(canvas, left, top, right, top + dp(78f), dp(17f), panelLight);
        drawCenteredText(canvas, label, (left + right) / 2f, top + dp(26f),
                sp(11f), textMuted, true);
        drawCenteredText(canvas, value, (left + right) / 2f, top + dp(57f),
                sp(19f), valueColor, true);
    }

    private void drawButton(Canvas canvas, RectF rect, int color, String text) {
        drawRoundedPanel(canvas, rect.left, rect.top, rect.right, rect.bottom,
                dp(18f), color);
        drawCenteredText(canvas, text, rect.centerX(), rect.centerY() + sp(6f),
                sp(15f), Color.WHITE, true);
    }

    private void drawOutlineButton(Canvas canvas, RectF rect, String text) {
        drawRoundedPanel(canvas, rect.left, rect.top, rect.right, rect.bottom,
                dp(15f), panelLight);
        stroke.setColor(roadLine);
        stroke.setStrokeWidth(dp(1.5f));
        canvas.drawRoundRect(rect, dp(15f), dp(15f), stroke);
        drawCenteredText(canvas, text, rect.centerX(), rect.centerY() + sp(5f),
                sp(14f), textPrimary, true);
    }

    private void drawRoundedPanel(Canvas canvas, float left, float top, float right,
                                  float bottom, float radius, int color) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), radius, radius, paint);
    }

    private void drawCenteredText(Canvas canvas, String text, float x, float baseline,
                                  float size, int color, boolean bold) {
        drawText(canvas, text, x, baseline, size, color, bold, Paint.Align.CENTER);
    }

    private void drawText(Canvas canvas, String text, float x, float baseline,
                          float size, int color, boolean bold, Paint.Align align) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        canvas.drawText(text, x, baseline, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchDownX = x;
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = x - touchDownX;
            if (screen == MENU) {
                if (startButton.contains(x, y)) startRun();
            } else if (screen == PLAYING) {
                if (pauseButton.contains(x, y)) {
                    screen = PAUSED;
                    setKeepScreenOn(false);
                    invalidate();
                } else if (Math.abs(dx) > dp(25f)) {
                    moveLane(dx > 0f ? 1 : -1);
                } else {
                    moveLane(x < width / 2f ? -1 : 1);
                }
            } else if (screen == PAUSED) {
                if (primaryButton.contains(x, y)) {
                    screen = PLAYING;
                    firstFrame = true;
                    setKeepScreenOn(true);
                    invalidate();
                } else if (homeButton.contains(x, y)) {
                    goHome();
                }
            } else if (screen == GAME_OVER) {
                if (primaryButton.contains(x, y)) startRun();
                else if (homeButton.contains(x, y)) goHome();
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void pauseFromActivity() {
        if (screen == PLAYING) {
            screen = PAUSED;
            setKeepScreenOn(false);
            invalidate();
        }
    }

    public boolean handleBackPressed() {
        if (screen == PLAYING) {
            screen = PAUSED;
            setKeepScreenOn(false);
            invalidate();
            return true;
        }
        if (screen == PAUSED || screen == GAME_OVER) {
            goHome();
            return true;
        }
        return false;
    }

    private void goHome() {
        screen = MENU;
        entities.clear();
        setKeepScreenOn(false);
        invalidate();
    }

    private float laneCenter(int lane) {
        float left = width * 0.18f;
        float right = width * 0.82f;
        return left + (right - left) * lane / 2f;
    }

    private float roadTop() {
        return Math.max(dp(130f), height * 0.20f);
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static final class Entity {
        final int type;
        final int lane;
        final int value;
        final String label;
        float y;

        Entity(int type, int lane, float y, int value, String label) {
            this.type = type;
            this.lane = lane;
            this.y = y;
            this.value = value;
            this.label = label;
        }
    }
}
