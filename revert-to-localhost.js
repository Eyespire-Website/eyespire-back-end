const fs = require('fs');
const path = require('path');

console.log('🔄 Reverting backend URLs to localhost for local testing...\n');

const LOCALHOST_BACKEND = 'http://localhost:8080';
const LOCALHOST_FRONTEND = 'http://localhost:3000';

const PRODUCTION_BACKEND = 'https://eyespire-back-end.onrender.com';
const PRODUCTION_FRONTEND = 'https://eyespire.vercel.app';

// Files to update
const filesToUpdate = [
    'src/main/resources/application.properties',
    'src/main/java/org/eyespire/eyespireapi/controller/AppointmentController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/AuthController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/DoctorController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/MedicalRecordController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/PaymentHistoryController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/OrderPaymentController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/OrderController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/PayOSController.java',
    'src/main/java/org/eyespire/eyespireapi/controller/UserController.java',
    'src/main/java/org/eyespire/eyespireapi/config/AppConfig.java',
    'src/main/java/org/eyespire/eyespireapi/config/WebSocketConfig.java',
    'src/main/java/org/eyespire/eyespireapi/service/EmailService.java',
    'src/main/java/org/eyespire/eyespireapi/service/OrderPaymentService.java',
    'src/main/java/org/eyespire/eyespireapi/service/UserService.java'
];

function updateFile(filePath) {
    try {
        if (!fs.existsSync(filePath)) {
            console.log(`⚠️  File not found: ${filePath}`);
            return;
        }

        let content = fs.readFileSync(filePath, 'utf8');
        let updated = false;

        // Replace production backend URL with localhost
        if (content.includes(PRODUCTION_BACKEND)) {
            content = content.replace(new RegExp(PRODUCTION_BACKEND.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), LOCALHOST_BACKEND);
            updated = true;
        }

        // Replace production frontend URL with localhost
        if (content.includes(PRODUCTION_FRONTEND)) {
            content = content.replace(new RegExp(PRODUCTION_FRONTEND.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), LOCALHOST_FRONTEND);
            updated = true;
        }

        if (updated) {
            fs.writeFileSync(filePath, content, 'utf8');
            console.log(`✅ Reverted: ${filePath}`);
        }
    } catch (error) {
        console.error(`❌ Error updating ${filePath}:`, error.message);
    }
}

// Update all files
filesToUpdate.forEach(updateFile);

console.log('\n✅ Backend URLs reverted to localhost!');
console.log('\n📝 Local testing URLs:');
console.log(`Frontend: ${LOCALHOST_FRONTEND}`);
console.log(`Backend: ${LOCALHOST_BACKEND}`);
console.log('\n🚀 Now you can test locally before production deployment!');
