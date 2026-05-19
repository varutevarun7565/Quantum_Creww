const express = require('express');
const router = express.Router();
const { getUsers, registerUser } = require('../controllers/userController');

// Define routes for /api/users
router.route('/').get(getUsers).post(registerUser);

module.exports = router;
