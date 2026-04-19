import os
import asyncio
import tempfile
import subprocess
from aiogram import Bot, Dispatcher, types
from aiogram.filters import Command
from aiogram.types import Message
import yt_dlp

# Токен вставлен прямо в код
BOT_TOKEN = "8690186576:AAHsEfhP9k1urNHSiPTlcSR_x6J0nzczLGY"

bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()

YDL_OPTS = {
    "format": "mp4/best",
    "outtmpl": "%(id)s.%(ext)s",
    "quiet": True,
    "no_warnings": True,
    "extract_flat": False,
    "noplaylist": True,
    "merge_output_format": "mp4",
}

@dp.message(Command("start"))
async def start_cmd(message: Message):
    await message.answer(
        "📎 Отправь мне ссылку на видео:\n"
        "• TikTok\n• Instagram Reels\n• YouTube Shorts\n• Twitter/X\n\n"
        "Я пришлю видео без водяного знака."
    )

@dp.message(lambda msg: msg.text and ("http://" in msg.text or "https://" in msg.text))
async def download_video(message: Message):
    url = message.text.strip()
    status_msg = await message.answer("⏳ Получаю информацию о видео...")

    with tempfile.TemporaryDirectory() as tmpdir:
        ydl_opts = YDL_OPTS.copy()
        ydl_opts["outtmpl"] = os.path.join(tmpdir, "%(id)s.%(ext)s")

        try:
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=False)

                duration = info.get("duration")
                if duration and duration > 600:
                    await status_msg.edit_text("❌ Видео длиннее 10 минут.")
                    return

                await status_msg.edit_text("⬇️ Скачиваю видео...")
                ydl.download([url])

                files = os.listdir(tmpdir)
                video_file = next((f for f in files if f.endswith((".mp4", ".mkv", ".webm"))), None)
                if not video_file:
                    await status_msg.edit_text("❌ Видео не найдено.")
                    return

                file_path = os.path.join(tmpdir, video_file)
                file_size = os.path.getsize(file_path)

                if file_size > 50 * 1024 * 1024:
                    await status_msg.edit_text("❌ Видео больше 50 МБ.")
                    return

                await status_msg.edit_text("📤 Отправляю...")
                with open(file_path, "rb") as video:
                    await message.reply_video(video, caption=f"✅ {info.get('title', 'Видео')}")
                await status_msg.delete()

        except Exception as e:
            await status_msg.edit_text(f"❌ Ошибка: {str(e)[:200]}")

async def main():
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())
