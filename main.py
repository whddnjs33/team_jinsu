from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
import uvicorn
import os

app = FastAPI()

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/upload-audio/")
async def upload_audio(file: UploadFile = File(...)):
    file_location = os.path.join(UPLOAD_DIR, file.filename)

    with open(file_location, "wb") as buffer:
        content = await file.read()
        buffer.write(content)

    print(f"📁 파일 저장됨: {file_location}")

    # 여기서 AI 분석 로직을 넣으면 됩니다
    result = "AI 분석 결과입니다!"

    return JSONResponse(content={"result": result})