@echo off
echo Setting environment variables...

set DB_PASSWORD=your_database_password
set GOOGLE_CLIENT_SECRET=your_google_client_secret
set AZURE_STORAGE_CONNECTION_STRING=your_azure_storage_connection_string
set EMAIL_PASSWORD=your_gmail_app_password
set PAYOS_API_KEY=your_payos_api_key
set PAYOS_CHECKSUM_KEY=your_payos_checksum_key
set GEMINI_API_KEY=your_gemini_api_key
set STORAGE_TYPE=azure
set UPLOAD_DIR=/tmp/eyespire/uploads

echo Environment variables set successfully!
echo Starting Spring Boot application...

mvn spring-boot:run
