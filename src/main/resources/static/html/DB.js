const express = require('express');
const mysql = require('mysql');
const dbconfig = require('./config/database.js');
const connection = mysql.createConnection(dbconfig);

const app = express();

// configuration =========================
app.set('port', process.env.PORT || 3000);

// app.get('/', (req, res) => {
//     res.send('Root');
// });

app.get('/nowprice', (req, res) => {
    connection.query('SELECT * from nowprice', (error, rows) => { // nowprice가 table 이름임
        if (error) throw error;
        console.log(rows['0']['price']); //rows[행 번호][열 이름]
        res.send(rows);
    });
});

// app.listen(app.get('port'), () => {
//     console.log('Express server listening on port ' + app.get('port'));
// });
