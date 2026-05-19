const User = require('../models/User');

const registerUser = async (req, res) => {
    try {
        const { userId, name, role, contactNumber, emergencyContact, bloodGroup, age, medicalCondition, password, photoUri } = req.body;

        const userExists = await User.findOne({ userId });
        if (userExists) {
            return res.status(409).json({ message: 'User already exists', user: null });
        }

        const user = await User.create({
            userId, name, role, contactNumber, emergencyContact, bloodGroup, age, medicalCondition, password, photoUri
        });

        res.status(201).json({ message: 'User registered successfully', user });
    } catch (error) {
        res.status(500).json({ message: error.message, user: null });
    }
};

const loginUser = async (req, res) => {
    try {
        const { userId, password } = req.body;
        const user = await User.findOne({ userId, password }); // Simple password check, hash in production!
        
        if (user) {
            res.status(200).json({ message: 'Login successful', user });
        } else {
            res.status(401).json({ message: 'Invalid credentials', user: null });
        }
    } catch (error) {
        res.status(500).json({ message: error.message, user: null });
    }
};

const updateProfile = async (req, res) => {
    try {
        const { userId } = req.body;
        const updatedUser = await User.findOneAndUpdate({ userId }, req.body, { new: true });
        
        if (updatedUser) {
            res.status(200).json({ message: 'Profile updated', user: updatedUser });
        } else {
            res.status(404).json({ message: 'User not found', user: null });
        }
    } catch (error) {
        res.status(500).json({ message: error.message, user: null });
    }
};

module.exports = { registerUser, loginUser, updateProfile };
