const express = require('express');
const dotenv = require('dotenv');
const cors = require('cors');
const connectDB = require('./config/db');

// Load env vars
dotenv.config();

// Connect to database
connectDB();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// Routes
app.use('/auth', require('./routes/authRoutes'));
app.use('/sos', require('./routes/sosRoutes'));
app.use('/patient', require('./routes/patientRoutes'));
app.use('/driver', require('./routes/driverRoutes'));

// Basic route for testing server
app.get('/', (req, res) => {
    res.json({ message: 'Welcome to the Jeevan API' });
});

// Custom Error Handler Middleware
app.use((err, req, res, next) => {
    const statusCode = res.statusCode ? res.statusCode : 500;
    res.status(statusCode);
    res.json({
        message: err.message,
        stack: process.env.NODE_ENV === 'production' ? null : err.stack,
    });
});

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
