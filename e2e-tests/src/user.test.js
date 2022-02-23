import fetch from 'node-fetch';

test('failed login', async () => {
    const response = await fetch(process.env.BASE_URL + '/authentication/login');
    expect(response.status).toBe(400);
});