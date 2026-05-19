const mongoose = require('mongoose');

const userSchema = mongoose.Schema(
    {
        userId: {
            type: String,
            required: [true, 'Please add a userId'],
            unique: true,
        },
        name: {
            type: String,
            required: [true, 'Please add a name'],
        },
        role: {
            type: String,
            enum: ['USER', 'DRIVER', 'HOSPITAL', 'ADMIN'],
            default: 'USER',
        },
        contactNumber: {
            type: String,
        },
        emergencyContact: {
            type: String,
        },
        bloodGroup: {
            type: String,
        },
        age: {
            type: String,
        },
        medicalCondition: {
            type: String,
        },
        password: {
            type: String,
            required: [true, 'Please add a password'],
        },
        photoUri: {
            type: String,
        }
    },
    {
        timestamps: true,
    }
);

module.exports = mongoose.model('User', userSchema);
