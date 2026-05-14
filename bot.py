import asyncio
import logging
from datetime import datetime, timedelta, timezone

import aiosqlite
from aiogram import Bot, Dispatcher, types
from aiogram.filters import Command
from aiogram.types import FSInputFile

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Дата ареста Алексея Навального (17 января 2021, UTC)
NAVALNY_ARREST_DATE = datetime(2021, 1, 17, 0, 0, 0, tzinfo=timezone.utc)
# 9:00 по Москве = 6:00 UTC
MORNING_HOUR_UTC = 6

# Токен твоего бота (жёстко прописан здесь)
BOT_TOKEN = "8993310963:AAGDsM_bU8DyVI3P3PwwDYaW34RhInfRNAs"

bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()

DB_PATH = "subscribers.db"

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

def get_time_since_arrest():
    now = datetime.now(timezone.utc)
    delta = now - NAVALNY_ARREST_DATE
    days = delta.days
    seconds = delta.seconds
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    secs = seconds % 60
    return days, hours, minutes, secs

@dp.message(Command("start"))
async def cmd_start(message: types.Message):
    # Сначала отправляем звуковой файл
audio = FSInputFile("start_sound.mp3")
await message.answer_audio(audio)

    # Затем подписываем и отправляем текстовое сообщение
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
            if any(word in str(e).lower() for word in ["blocked", "chat not found", "deactivated"]):
                await remove_subscriber(chat_id)

async def scheduler():
    while True:
        now = datetime.now(timezone.utc)
        target = now.replace(hour=MORNING_HOUR_UTC, minute=0, second=0, microsecond=0)
        if now > target:
            target += timedelta(days=1)
        wait_seconds = (target - now).total_seconds()
        logger.info(f"Следующая рассылка через {wait_seconds:.0f} сек, в {target} UTC")
        await asyncio.sleep(wait_seconds)
        await send_daily_message()

async def main():
    print("=== БОТ ЗАПУСКАЕТСЯ ===")
    await init_db()
    asyncio.create_task(scheduler())
    await dp.start_polling(bot, skip_updates=True)

if __name__ == "__main__":
    asyncio.run(main())
