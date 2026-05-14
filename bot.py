import asyncio
import logging
import os
from datetime import datetime, timedelta, timezone

import aiosqlite
from aiogram import Bot, Dispatcher, types
from aiogram.filters import Command

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Дата ареста Алексея Навального (17 января 2021, UTC)
NAVALNY_ARREST_DATE = datetime(2021, 1, 17, 0, 0, 0, tzinfo=timezone.utc)
# 9:00 по Москве = 6:00 UTC
MORNING_HOUR_UTC = 6

# Токен бота берём из переменной окружения (будет задана в Railway)
BOT_TOKEN = os.getenv("BOT_TOKEN")
if not BOT_TOKEN:
    raise ValueError("Не задана переменная окружения BOT_TOKEN")

# Инициализация бота и диспетчера
bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()

DB_PATH = "subscribers.db"

# --- База данных ---
async def init_db():
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("CREATE TABLE IF NOT EXISTS subscribers (chat_id INTEGER PRIMARY KEY)")
        await db.commit()

async def add_subscriber(chat_id: int):
    try:
        async with aiosqlite.connect(DB_PATH) as db:
            await db.execute("INSERT OR IGNORE INTO subscribers (chat_id) VALUES (?)", (chat_id,))
            await db.commit()
    except Exception as e:
        logger.error(f"Ошибка добавления подписчика {chat_id}: {e}")

async def get_all_subscribers():
    async with aiosqlite.connect(DB_PATH) as db:
        cursor = await db.execute("SELECT chat_id FROM subscribers")
        rows = await cursor.fetchall()
        return [row[0] for row in rows]

async def remove_subscriber(chat_id: int):
    try:
        async with aiosqlite.connect(DB_PATH) as db:
            await db.execute("DELETE FROM subscribers WHERE chat_id = ?", (chat_id,))
            await db.commit()
    except Exception as e:
        logger.error(f"Ошибка удаления подписчика {chat_id}: {e}")

# --- Расчёт времени ---
def get_time_since_arrest():
    now = datetime.now(timezone.utc)
    delta = now - NAVALNY_ARREST_DATE
    days = delta.days
    seconds = delta.seconds
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    secs = seconds % 60
    return days, hours, minutes, secs

# --- Обработчики команд ---
@dp.message(Command("start"))
async def cmd_start(message: types.Message):
    await add_subscriber(message.chat.id)
    days, hours, minutes, secs = get_time_since_arrest()
    text = (
        f"Привет! Я бот, который считает, сколько времени Алексей Навальный находится в заключении.\n\n"
        f"📅 Арестован 17 января 2021 года.\n\n"
        f"Сейчас прошло: {days} дней, {hours} часов, {minutes} минут, {secs} секунд.\n\n"
        f"Ты подписан на ежедневную рассылку в 9:00 МСК. Используй /days, чтобы узнать актуальное время в любой момент."
    )
    await message.answer(text)

@dp.message(Command("days"))
async def cmd_days(message: types.Message):
    days, hours, minutes, secs = get_time_since_arrest()
    text = f"Алексей Навальный в заключении:\n{days} дней, {hours} часов, {minutes} минут, {secs} секунд."
    await message.answer(text)

# --- Ежедневная рассылка ---
async def send_daily_message():
    subscribers = await get_all_subscribers()
    days, hours, minutes, secs = get_time_since_arrest()
    text = (
        f"🗓 Ежедневное напоминание:\n"
        f"Алексей Навальный находится в заключении уже {days} дней.\n"
        f"Точное время: {days} дн., {hours} ч., {minutes} мин., {secs} сек.\n\n"
        f"Не забывайте. #СвободуНавальному"
    )
    for chat_id in subscribers:
        try:
            await bot.send_message(chat_id, text)
        except Exception as e:
            logger.warning(f"Не удалось отправить сообщение {chat_id}: {e}")
            # Если пользователь заблокировал бота или удалил аккаунт – удаляем из базы
            if any(word in str(e).lower() for word in ["blocked", "chat not found", "deactivated"]):
                await remove_subscriber(chat_id)

# --- Планировщик (рассылка каждый день в 9:00 МСК) ---
async def scheduler():
    while True:
        now = datetime.now(timezone.utc)
        target = now.replace(hour=MORNING_HOUR_UTC, minute=0, second=0, microsecond=0)
        if now > target:
            target += timedelta(days=1)  # ждём до завтрашнего утра
        wait_seconds = (target - now).total_seconds()
        logger.info(f"Следующая рассылка через {wait_seconds:.0f} сек, в {target} UTC")
        await asyncio.sleep(wait_seconds)
        await send_daily_message()

# --- Запуск ---
async def main():
    await init_db()
    # Запускаем планировщик в фоновом режиме
    asyncio.create_task(scheduler())
    # Стартуем получение сообщений от Telegram
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())